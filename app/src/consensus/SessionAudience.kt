package lerpmusic.website.consensus

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.collectAddedInChildCoroutine
import lerpmusic.consensus.utils.flatMapMergeAddedInChildCoroutine
import mu.KotlinLogging

/**
 * Аудитория — несколько слушателей.
 */
class SessionAudience(
    private val sessionScope: CoroutineScope,
) : Audience {
    private val activeListeners: MutableStateFlow<List<SessionListener>> = MutableStateFlow(emptyList())

    init {
        sessionScope.launch {
            activeListeners.collectAddedInChildCoroutine { it.receiveMessages() }
        }
    }

    override val listenersCount: StateFlow<Int> =
        activeListeners
            .map { it.size }
            .onEach { log.info { "listenersCount: $it" } }
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = 0)

    /**
     * Добавление слушателя в сессию.
     *
     * Удаление произойдёт автоматически при отключении слушателя.
     */
    fun addListener(connection: ListenerConnection) {
        sessionScope.launch(CoroutineName("ListenerConnectionCompletionHandler")) {
            val newListener = SessionListener(
                connection = connection,
            )

            activeListeners.update { listeners ->
                check(listeners.none { it.connection.id == connection.id }) { "Connection $connection already exists" }
                listeners + newListener
            }
            log.info { "Listener ${connection.id} connected" }

            // При отключении слушателя удаляем его из списка
            try {
                connection.coroutineContext.job.join()
            } finally {
                activeListeners.update { listeners -> listeners - newListener }
                log.info { "Listener ${connection.id} disconnected" }
            }
        }
    }

    override val intensityUpdates: Flow<IntensityUpdate> =
        activeListeners.flatMapMergeAddedInChildCoroutine { it.intensityUpdates }

    override suspend fun shouldPlayNote(note: Note): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelNote(note: Note) {
        TODO("Not yet implemented")
    }
}

/**
 * Один слушатель.
 */
private class SessionListener(
    val connection: ListenerConnection,
) : Audience, CoroutineScope by connection {
    override val listenersCount: Flow<Int> = flowOf(1)

    /**
     * Небольшой абьюз [SharedFlow] и [SharingStarted.WhileSubscribed]:
     * - при первой подписке на [intensityUpdates] будет отправлен запрос слушателю
     * - при последней отписке слушателю отправится уведомление об отмене
     * - если подписки нет, сообщения от слушателя бросаются на пол
     *
     * Эта конструкция заменяет атомарный флаг + дублирование логики подписки/отписки в [intensityUpdates]
     */
    private val receivedIntensityUpdates: StateFlow<MutableSharedFlow<IntensityUpdate>?> = flow {
        // 1. начинаем слушать сообщения от слушателя
        // Это происходит до отправки запроса, чтобы не просыпать никакие ответы
        emit(MutableSharedFlow<IntensityUpdate>())

        // 2. уведомляем слушателя о том, что хотим получать уведомления об интенсивности.
        connection.send(ListenerResponse.ReceiveIntensityUpdates)

        try {
            // 3. слушаем бесконечно — пока кто-то подписан на эти уведомления
            awaitCancellation()
        } finally {
            // 4. Компенсация шага 1 — отменяем уведомление,
            connection.launch {
                connection.send(ListenerResponse.CancelIntensityUpdates)
            }
        }
    }.stateIn(connection, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    /**
     * Изменения интенсивности
     */
    override val intensityUpdates: Flow<IntensityUpdate> = receivedIntensityUpdates.flatMapLatest { it ?: emptyFlow() }

    suspend fun receiveMessages(): Nothing {
        while (true) {
            when (val event = connection.receive()) {
                ListenerRequest.Action -> {}
                ListenerRequest.DecreaseIntensity -> receivedIntensityUpdates.value?.emit(IntensityUpdate(1.0, 0.0))
                ListenerRequest.IncreaseIntensity -> receivedIntensityUpdates.value?.emit(IntensityUpdate(0.0, 1.0))
            }
        }
    }

    override suspend fun shouldPlayNote(note: Note): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelNote(note: Note) {
        TODO("Not yet implemented")
    }
}

class ListenerConnection(
    val id: String,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope,
) : CoroutineScope by coroutineScope {
    suspend fun send(data: ListenerResponse): Unit = webSocketSession.sendSerialized(data)
    suspend fun receive(): ListenerRequest = webSocketSession.receiveDeserialized()
}

private val log = KotlinLogging.logger {}