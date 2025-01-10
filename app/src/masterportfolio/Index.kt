package lerpmusic.website.masterportfolio

import kotlinx.html.*

fun HTML.indexPage() {
    head {
        title("Максим Петелин")
    }

    body {
        h1 { +"Портфолио" }

        aboutMe()
        portfolio()
        motivation()
    }
}

private fun FlowContent.aboutMe() {
    val text = """
        Привет. Меня зовут Максим Петелин, я бакалавр по специальности
        Программная инженерия, в данный момент занимаюсь серверной разработкой
        в крупном сервисе для рекламодателей.
    """.trimIndent()

    p { +text }
}

private fun FlowContent.portfolio() {
    h2 { +"Мои работы" }

    enumValues<PortfolioEntry>()
        .forEach(::render)
}

private fun FlowContent.motivation() {
    h2 { +"Мотивационное письмо" }

    p { +"Считаю, что я обладаю следующими навыками:" }
    ul {
        li { +"Продвинутая работа в Ableton Live" }
        li {
            +"""
                Понимаю устройство и принципы работы виртуально-аналоговых,
                таблично-волновых и FM синтезаторов, различных аудио-эффектов
            """.trimIndent()
        }
        li { +"Разбираюсь в технической стороне вопроса написания музыкальных композиций" }
    }

    p { +"Область моих интересов: генеративная музыка, синтез, создание DIY синтезаторов и разного смежного софта." }

    p {
        +"""
            Магистратура Sound Art & Sound Studies привлекает меня набором дисциплин.
            Несмотря на то, что я давно увлекаюсь написанием музыки и программированием,
            большая часть тем, изучаемых на программе, для меня будут новыми.
            К примеру, я никогда не пробовал работать в визуальных средах программирования
            типа Max, Pure Data, Supercollider, у меня нет теоретической базы,
            так как я изучал многие темы методом “научного тыка”.
        """.trimIndent()
    }

    p {
        +"""
            Я считаю, что обучение здесь позволит мне реализовать свои амбиции и попробовать
            себя в новых областях. Возможно, с полученными знаниями у меня получится построить
            более интересную и творческую карьеру.
        """.trimIndent()
    }

    p {
        +"""
            Также я рассматриваю магистратуру Sound Art & Sound Studies как возможность попасть
            в окружение творческих, мотивированных людей, с некоторыми из которых я,
            возможно, буду в дальнейшем работать и заниматься совместными проектами.
        """.trimIndent()
    }
}