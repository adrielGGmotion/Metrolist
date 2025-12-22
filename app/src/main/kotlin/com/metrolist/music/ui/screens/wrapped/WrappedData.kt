package com.metrolist.music.ui.screens.wrapped

data class MessagePair(val range: LongRange, val tease: String, val reveal: String)

object WrappedRepository {
    private val messages = listOf(
        MessagePair(0L..999L, "I really hope you are not dissapointed...", "Less than 1 day. Just warming up?"),
        MessagePair(0L..999L, "Testing the waters, are we?", "A quick dip in the musical ocean."),
        MessagePair(0L..999L, "Busy schedule this year?", "Short, sweet, and to the point."),
        MessagePair(0L..999L, "Silence is golden, they say...", "But you preferred a little bit of noise."),

        MessagePair(1000L..4999L, "It seems like you found Metrolist recently...", "And you dedicated a few precious moments to the tunes."),
        MessagePair(1000L..4999L, "You have a life outside of music.", "A healthy balance. We respect that."),
        MessagePair(1000L..4999L, "Not too quiet, not too loud.", "Just the right amount of vibes."),
        MessagePair(1000L..4999L, "A casual stop on your journey.", "Thanks for dropping by."),

        MessagePair(5000L..14999L, "Music is definitely your thing.", "A solid soundtrack for your year."),
        MessagePair(5000L..14999L, "We saw you here quite a bit.", "Always setting the mood."),
        MessagePair(5000L..14999L, "Your commute must be fun.", "Miles and miles of melodies."),
        MessagePair(5000L..14999L, "Consistent. Reliable. Rhythmic.", "You know what you like."),

        MessagePair(15000L..39999L, "Do you ever take your headphones off?", "Music is basically your oxygen."),
        MessagePair(15000L..39999L, "Your battery is begging for mercy.", "But your ears absolutely love it."),
        MessagePair(15000L..39999L, "Main Character Energy detected.", "Your life is literally a movie."),
        MessagePair(15000L..39999L, "Walking, working, sleeping...", "There was always a song playing."),

        MessagePair(40000L..Long.MAX_VALUE, "Are you... okay?", "You literally live here now."),
        MessagePair(40000L..Long.MAX_VALUE, "We are worried about your eardrums.", "Top 1% behavior. Absolute legend."),
        MessagePair(40000L..Long.MAX_VALUE, "Silence scares you, doesn't it?", "A wall of sound, all year long."),
        MessagePair(40000L..Long.MAX_VALUE, "Certified Stress Tester.", "You made those extractors work overtime.")
    )

    fun getMessage(minutes: Long): MessagePair {
        val possibleMessages = messages.filter { minutes in it.range }
        return if (possibleMessages.isNotEmpty()) {
            possibleMessages.random()
        } else {
            // Fallback for safety
            MessagePair(0L..Long.MAX_VALUE, "Looks like we lost count!", "But you definitely listened to a lot of music.")
        }
    }
}
