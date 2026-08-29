package com.cardscanner.parser

import com.cardscanner.CardScannerResult
import java.util.Locale
import kotlin.math.ceil

internal object CardOcrParser {

    private const val MIN_CARD_NUMBER_LENGTH = 13
    private const val MAX_CARD_NUMBER_LENGTH = 19
    private const val MAX_NUMERIC_RUN_LENGTH = 24

    /*
     * A tolerant candidate must still contain enough characters that OCR
     * recognized as real digits. Corrections are never trusted by themselves.
     */
    private const val MIN_REAL_DIGITS = 8
    private const val MIN_REAL_DIGIT_RATIO = 0.55f

    private const val MIN_CARDHOLDER_NAME_SCORE = 55

    private const val CARD_SEPARATOR_PATTERN =
        "[\\s\\p{Pd}\\x{00B7}.]*"

    private val strictCardNumberRunRegex =
        Regex(
            pattern =
                "(?<!\\d)(?:\\d$CARD_SEPARATOR_PATTERN)" +
                        "{$MIN_CARD_NUMBER_LENGTH,$MAX_NUMERIC_RUN_LENGTH}(?!\\d)"
        )

    private val tolerantCardNumberRunRegex =
        Regex(
            pattern =
                "(?<![A-Z0-9])" +
                        "(?:[0-9OQDISZGBLT|!]$CARD_SEPARATOR_PATTERN)" +
                        "{$MIN_CARD_NUMBER_LENGTH,$MAX_NUMERIC_RUN_LENGTH}" +
                        "(?![A-Z0-9])",
            option = RegexOption.IGNORE_CASE
        )

    private val expiryLabelRegex =
        Regex(
            pattern =
                "\\b(?:" +
                        "EXP(?:IRY|IRES|DATE)?|" +
                        "VALID\\s+(?:THRU|THROUGH)|" +
                        "GOOD\\s+(?:THRU|THROUGH)|" +
                        "THRU|" +
                        "MM\\s*/?\\s*YY" +
                        ")\\b",
            option = RegexOption.IGNORE_CASE
        )

    private val expiryWithSlashRegex =
        Regex(
            pattern =
                "(?<!\\d)" +
                        "(0[1-9]|1[0-2])" +
                        "\\s*/\\s*" +
                        "(20\\d{2}|\\d{2})" +
                        "(?!\\d)"
        )

    private val separatedExpiryRegex =
        Regex(
            pattern =
                "(?<!\\d)" +
                        "(0[1-9]|1[0-2])" +
                        "(?:\\s*[/\\-.]\\s*|\\s+)" +
                        "(20\\d{2}|\\d{2})" +
                        "(?!\\d)"
        )

    private val compactExpiryRegex =
        Regex(
            pattern =
                "(?<!\\d)" +
                        "(0[1-9]|1[0-2])" +
                        "(\\d{2})" +
                        "(?!\\d)"
        )

    private val nonNameCharacterRegex =
        Regex("[^\\p{L} .'-]")

    private val multipleWhitespaceRegex =
        Regex("\\s+")

    private val ignoredNameKeywords =
        listOf(
            "VISA",
            "MASTERCARD",
            "MASTER CARD",
            "AMERICAN EXPRESS",
            "AMEX",
            "DISCOVER",
            "DINERS",
            "DINERS CLUB",
            "JCB",
            "UNIONPAY",
            "MAESTRO",
            "RUPAY",

            "DEBIT",
            "CREDIT",
            "PREPAID",
            "BUSINESS",
            "CORPORATE",
            "COMMERCIAL",

            "VALID",
            "VALID THRU",
            "VALID THROUGH",
            "VALID FROM",
            "GOOD THRU",
            "GOOD THROUGH",
            "EXPIRES",
            "EXPIRY",
            "EXP DATE",
            "EXP",
            "MONTH",
            "YEAR",
            "MM",
            "YY",
            "MM YY",

            "BANK",
            "BANKING",
            "FINANCIAL",
            "CREDIT UNION",
            "CARD",
            "CARDHOLDER",
            "CARD HOLDER",
            "CARDHOLDER NAME",
            "NAME",
            "PLATINUM",
            "CLASSIC",
            "GOLD",
            "SIGNATURE",
            "INFINITE",
            "ELECTRON",
            "WORLD",
            "WORLD ELITE",
            "ELITE",
            "REWARDS",
            "PREFERRED",
            "CUSTOMER",
            "MEMBER",
            "MEMBER SINCE",
            "SINCE",
            "THRU",
            "CONTACTLESS",
            "PAYPASS",
            "PAYWAVE",
            "CASHBACK",
            "CASH BACK",
            "TRAVEL",
            "MILES",

            "AUTHORIZED",
            "AUTHORIZED USER",
            "USE ONLY",
            "NOT VALID",
            "SEE BACK",
            "SECURITY",
            "SERVICE",
            "INTERNATIONAL",
            "ELECTRONIC USE ONLY",
            "MEMBER FDIC"
        )

    private val ignoredNameKeywordRegexes =
        ignoredNameKeywords.map { keyword ->
            Regex(
                pattern =
                    "(?<!\\p{L})" +
                            Regex.escape(keyword) +
                            "(?!\\p{L})",
                option = RegexOption.IGNORE_CASE
            )
        }

    fun parse(text: String): CardScannerResult? {
        if (text.isBlank()) {
            return null
        }

        val cardNumber =
            extractCardNumber(text)
                ?: return null

        val expiryDate =
            extractExpiryDate(text)

        val cardHolderName =
            extractCardHolderName(
                text = text,
                cardNumber = cardNumber,
                expiryDate = expiryDate
            )

        return CardScannerResult(
            cardNumber = cardNumber,
            expiryDate = expiryDate,
            cardHolderName = cardHolderName
        )
    }

    private fun extractCardNumber(
        text: String
    ): String? {
        val searchableText =
            text.replace('\u00A0', ' ')

        val candidates =
            mutableMapOf<String, NumberCandidate>()

        collectNumberCandidates(
            text = searchableText,
            regex = strictCardNumberRunRegex,
            allowCorrections = false,
            strictSource = true,
            destination = candidates
        )

        collectNumberCandidates(
            text = searchableText,
            regex = tolerantCardNumberRunRegex,
            allowCorrections = true,
            strictSource = false,
            destination = candidates
        )

        return candidates
            .values
            .maxWithOrNull(
                compareBy<NumberCandidate> { candidate ->
                    candidate.score
                }.thenBy { candidate ->
                    candidate.value.length
                }
            )
            ?.value
    }

    private fun collectNumberCandidates(
        text: String,
        regex: Regex,
        allowCorrections: Boolean,
        strictSource: Boolean,
        destination: MutableMap<String, NumberCandidate>
    ) {
        regex.findAll(text).forEach { match ->
            val tokens =
                match.value.toNumberTokens(
                    allowCorrections = allowCorrections
                )

            if (tokens.size < MIN_CARD_NUMBER_LENGTH) {
                return@forEach
            }

            val maximumCandidateLength =
                minOf(
                    MAX_CARD_NUMBER_LENGTH,
                    tokens.size
                )

            for (
            length in maximumCandidateLength
                    downTo MIN_CARD_NUMBER_LENGTH
            ) {
                val finalStartIndex =
                    tokens.size - length

                for (startIndex in 0..finalStartIndex) {
                    val endIndex =
                        startIndex + length

                    val candidateTokens =
                        tokens.subList(
                            startIndex,
                            endIndex
                        )

                    val correctedCount =
                        candidateTokens.count { token ->
                            token.corrected
                        }

                    val realDigitCount =
                        length - correctedCount

                    if (allowCorrections) {
                        val requiredRealDigitCount =
                            maxOf(
                                MIN_REAL_DIGITS,
                                ceil(
                                    length *
                                            MIN_REAL_DIGIT_RATIO
                                ).toInt()
                            )

                        if (
                            realDigitCount <
                            requiredRealDigitCount
                        ) {
                            continue
                        }
                    }

                    val number =
                        candidateTokens.joinToString(
                            separator = ""
                        ) { token ->
                            token.digit.toString()
                        }

                    if (!isValidCardNumber(number)) {
                        continue
                    }

                    val isFullRun =
                        startIndex == 0 &&
                                endIndex == tokens.size

                    val score =
                        scoreNumberCandidate(
                            number = number,
                            rawRun = match.value,
                            strictSource = strictSource,
                            correctedCount = correctedCount,
                            realDigitCount = realDigitCount,
                            isFullRun = isFullRun
                        )

                    val existing =
                        destination[number]

                    if (
                        existing == null ||
                        score > existing.score
                    ) {
                        destination[number] =
                            NumberCandidate(
                                value = number,
                                score = score
                            )
                    }
                }
            }
        }
    }

    private fun scoreNumberCandidate(
        number: String,
        rawRun: String,
        strictSource: Boolean,
        correctedCount: Int,
        realDigitCount: Int,
        isFullRun: Boolean
    ): Int {
        var score = 0

        if (strictSource) {
            score += 50
        }

        score +=
            realDigitCount * 3

        score -=
            correctedCount * 8

        if (isFullRun) {
            score += 35
        }

        score +=
            when (number.length) {
                16 -> 25
                15 -> 20
                19 -> 15
                18 -> 12
                17 -> 10
                14 -> 8
                13 -> 8
                else -> 0
            }

        if (number.firstOrNull() in '3'..'6') {
            score += 8
        }

        if (isFullRun) {
            score +=
                groupingScore(
                    rawRun = rawRun,
                    numberLength = number.length
                )
        }

        return score
    }

    private fun groupingScore(
        rawRun: String,
        numberLength: Int
    ): Int {
        val groups =
            rawRun
                .trim()
                .split(
                    Regex(
                        "[\\s\\p{Pd}\\x{00B7}.]+"
                    )
                )
                .filter { group ->
                    group.isNotBlank()
                }

        if (groups.size <= 1) {
            return 0
        }

        val groupLengths =
            groups.map { group ->
                group.toNumberTokens(
                    allowCorrections = true
                ).size
            }

        return when {
            numberLength == 16 &&
                    groupLengths == listOf(4, 4, 4, 4) ->
                20

            numberLength == 15 &&
                    groupLengths == listOf(4, 6, 5) ->
                20

            numberLength == 19 &&
                    groupLengths == listOf(4, 4, 4, 4, 3) ->
                18

            groupLengths.all { length ->
                length in 3..6
            } ->
                8

            else ->
                0
        }
    }

    private fun String.toNumberTokens(
        allowCorrections: Boolean
    ): List<NumberToken> {
        val tokens =
            ArrayList<NumberToken>(length)

        forEach { character ->
            val realDigit =
                character.toAsciiDigitOrNull()

            if (realDigit != null) {
                tokens.add(
                    NumberToken(
                        digit = realDigit,
                        corrected = false
                    )
                )

                return@forEach
            }

            if (allowCorrections) {
                val correctedDigit =
                    confusableCharacterToDigit(
                        character
                    )

                if (correctedDigit != null) {
                    tokens.add(
                        NumberToken(
                            digit = correctedDigit,
                            corrected = true
                        )
                    )
                }
            }
        }

        return tokens
    }

    private fun extractExpiryDate(
        text: String
    ): String? {
        val lines =
            text
                .lineSequence()
                .map { line ->
                    line.trim()
                }
                .filter { line ->
                    line.isNotEmpty()
                }
                .toList()

        /*
         * First preference: an expiry value near a recognized expiry label.
         * This also supports values such as 1228 and 12 28.
         */
        lines.forEachIndexed { index, line ->
            val labelMatch =
                expiryLabelRegex.find(line)
                    ?: return@forEachIndexed

            val searchWindow =
                buildString {
                    append(
                        line.substring(
                            labelMatch.range.last + 1
                        )
                    )

                    for (
                    nextIndex in
                    index + 1..minOf(
                        index + 2,
                        lines.lastIndex
                    )
                    ) {
                        append(' ')
                        append(lines[nextIndex])
                    }
                }

            val normalizedWindow =
                normalizeNumericSearchText(
                    searchWindow
                )

            separatedExpiryRegex
                .find(normalizedWindow)
                ?.toExpiryDate()
                ?.let { expiry ->
                    return expiry
                }

            compactExpiryRegex
                .find(normalizedWindow)
                ?.toExpiryDate()
                ?.let { expiry ->
                    return expiry
                }
        }

        /*
         * A slash is a strong expiry indicator, so it is safe to search the
         * complete OCR output for MM/YY or MM/YYYY.
         */
        val normalizedText =
            normalizeNumericSearchText(text)

        expiryWithSlashRegex
            .find(normalizedText)
            ?.toExpiryDate()
            ?.let { expiry ->
                return expiry
            }

        /*
         * Hyphen, dot, and space-only formats are accepted only on short
         * numeric-looking lines to avoid reading part of a card number as an
         * expiry date.
         */
        lines.forEach { line ->
            val normalizedLine =
                normalizeNumericSearchText(line)

            val digitCount =
                normalizedLine.count { character ->
                    character in '0'..'9'
                }

            if (digitCount !in 4..6) {
                return@forEach
            }

            separatedExpiryRegex
                .find(normalizedLine)
                ?.toExpiryDate()
                ?.let { expiry ->
                    return expiry
                }
        }

        return null
    }

    private fun MatchResult.toExpiryDate(): String? {
        val month =
            groupValues.getOrNull(1)
                ?.toIntOrNull()
                ?: return null

        val rawYear =
            groupValues.getOrNull(2)
                ?.takeIf { value ->
                    value.isNotBlank()
                }
                ?: return null

        if (month !in 1..12) {
            return null
        }

        val twoDigitYear =
            rawYear.takeLast(2)

        if (
            twoDigitYear.length != 2 ||
            twoDigitYear.any { character ->
                character !in '0'..'9'
            }
        ) {
            return null
        }

        return "%02d/%s".format(
            Locale.US,
            month,
            twoDigitYear
        )
    }

    private fun normalizeNumericSearchText(
        text: String
    ): String {
        return buildString(text.length) {
            text.forEach { character ->
                val realDigit =
                    character.toAsciiDigitOrNull()

                when {
                    realDigit != null ->
                        append(realDigit)

                    confusableCharacterToDigit(
                        character
                    ) != null ->
                        append(
                            confusableCharacterToDigit(
                                character
                            )
                        )

                    character == '/' ->
                        append('/')

                    character == '.' ->
                        append('.')

                    character == '-' ||
                            character == '\u2013' ||
                            character == '\u2014' ||
                            character == '\u2212' ->
                        append('-')

                    character.isWhitespace() ->
                        append(' ')

                    else ->
                        append(' ')
                }
            }
        }
    }

    private fun extractCardHolderName(
        text: String,
        cardNumber: String,
        expiryDate: String?
    ): String? {
        val rawLines =
            text
                .lineSequence()
                .map { line ->
                    line.trim()
                }
                .filter { line ->
                    line.isNotEmpty()
                }
                .toList()

        if (rawLines.isEmpty()) {
            return null
        }

        val cardNumberLineIndex =
            findCardNumberLineIndex(
                lines = rawLines,
                cardNumber = cardNumber
            )

        val expiryLineIndex =
            findExpiryLineIndex(
                lines = rawLines,
                expiryDate = expiryDate
            )

        val candidates =
            mutableListOf<NameCandidate>()

        rawLines.forEachIndexed { index, rawLine ->
            if (
                rawLine.any { character ->
                    character.isDigit()
                }
            ) {
                return@forEachIndexed
            }

            val upperLine =
                rawLine.uppercase(Locale.US)

            if (
                ignoredNameKeywordRegexes.any { regex ->
                    regex.containsMatchIn(upperLine)
                }
            ) {
                return@forEachIndexed
            }

            val cleanLine =
                upperLine
                    .replace(
                        nonNameCharacterRegex,
                        " "
                    )
                    .replace(
                        multipleWhitespaceRegex,
                        " "
                    )
                    .trim(
                        ' ',
                        '.',
                        '-',
                        '\''
                    )

            if (cleanLine.length !in 5..40) {
                return@forEachIndexed
            }

            val parts =
                cleanLine
                    .split(' ')
                    .filter { part ->
                        part.isNotBlank()
                    }

            if (parts.size !in 2..5) {
                return@forEachIndexed
            }

            val looksLikeName =
                parts.all { part ->
                    val letterCount =
                        part.count { character ->
                            character.isLetter()
                        }

                    letterCount >= 2 &&
                            part.length in 2..20 &&
                            part.all { character ->
                                character.isLetter() ||
                                        character == '\'' ||
                                        character == '.' ||
                                        character == '-'
                            }
                }

            if (!looksLikeName) {
                return@forEachIndexed
            }

            var score =
                when (parts.size) {
                    2 -> 35
                    3 -> 28
                    4 -> 20
                    5 -> 12
                    else -> 0
                }

            val letterCount =
                cleanLine.count { character ->
                    character.isLetter()
                }

            val visibleCharacterCount =
                cleanLine.count { character ->
                    !character.isWhitespace()
                }
                    .coerceAtLeast(1)

            val letterRatio =
                letterCount.toFloat() /
                        visibleCharacterCount.toFloat()

            if (letterRatio >= 0.90f) {
                score += 20
            } else if (letterRatio >= 0.80f) {
                score += 12
            }

            if (cleanLine.length in 7..30) {
                score += 15
            }

            if (index >= rawLines.size / 2) {
                score += 10
            }

            if (cardNumberLineIndex >= 0) {
                val distanceFromNumber =
                    index - cardNumberLineIndex

                score +=
                    when (distanceFromNumber) {
                        1 -> 35
                        2 -> 30
                        3 -> 20
                        4 -> 10
                        in Int.MIN_VALUE..0 -> -15
                        else -> 0
                    }
            }

            if (expiryLineIndex >= 0) {
                val distanceFromExpiry =
                    index - expiryLineIndex

                score +=
                    when (distanceFromExpiry) {
                        1 -> 25
                        2 -> 18
                        3 -> 10
                        else -> 0
                    }
            }

            if (
                rawLine.none { character ->
                    character.isLowerCase()
                }
            ) {
                score += 5
            }

            if (cleanLine.contains('.')) {
                score -= 5
            }

            if (cleanLine.contains('-')) {
                score -= 3
            }

            if (
                parts.distinct().size !=
                parts.size
            ) {
                score -= 15
            }

            candidates.add(
                NameCandidate(
                    value = cleanLine,
                    index = index,
                    score = score
                )
            )
        }

        return candidates
            .maxWithOrNull(
                compareBy<NameCandidate> { candidate ->
                    candidate.score
                }.thenByDescending { candidate ->
                    candidate.index
                }
            )
            ?.takeIf { candidate ->
                candidate.score >=
                        MIN_CARDHOLDER_NAME_SCORE
            }
            ?.value
    }

    private fun findCardNumberLineIndex(
        lines: List<String>,
        cardNumber: String
    ): Int {
        val numberTail =
            cardNumber.takeLast(8)

        return lines.indexOfFirst { line ->
            normalizeDigitLikeOnly(line)
                .contains(numberTail)
        }
    }

    private fun findExpiryLineIndex(
        lines: List<String>,
        expiryDate: String?
    ): Int {
        val expiryDigits =
            expiryDate
                ?.filter { character ->
                    character.isDigit()
                }
                ?.takeIf { value ->
                    value.length == 4
                }
                ?: return -1

        return lines.indexOfFirst { line ->
            normalizeDigitLikeOnly(line)
                .contains(expiryDigits)
        }
    }

    private fun normalizeDigitLikeOnly(
        text: String
    ): String {
        return buildString(text.length) {
            text.forEach { character ->
                val digit =
                    character.toAsciiDigitOrNull()
                        ?: confusableCharacterToDigit(
                            character
                        )

                if (digit != null) {
                    append(digit)
                }
            }
        }
    }

    private fun Char.toAsciiDigitOrNull(): Char? {
        if (this in '0'..'9') {
            return this
        }

        val numericValue =
            Character.digit(this, 10)

        return if (numericValue in 0..9) {
            ('0'.code + numericValue).toChar()
        } else {
            null
        }
    }

    private fun confusableCharacterToDigit(
        character: Char
    ): Char? {
        return when (
            character.uppercaseChar()
        ) {
            'O', 'Q', 'D' -> '0'
            'I', 'L', '|', '!' -> '1'
            'Z' -> '2'
            'S' -> '5'
            'G' -> '6'
            'T' -> '7'
            'B' -> '8'
            else -> null
        }
    }

    private fun isValidCardNumber(
        number: String
    ): Boolean {
        if (
            number.length !in
            MIN_CARD_NUMBER_LENGTH..MAX_CARD_NUMBER_LENGTH
        ) {
            return false
        }

        /*
         * Avoid accepting values such as 0000000000000000, which pass Luhn
         * mathematically but are not useful card numbers.
         */
        if (number.toSet().size < 2) {
            return false
        }

        return luhnCheck(number)
    }

    private fun luhnCheck(
        number: String
    ): Boolean {
        var sum = 0
        var doubleDigit = false

        for (index in number.lastIndex downTo 0) {
            var digit =
                number[index]
                    .digitToIntOrNull()
                    ?: return false

            if (doubleDigit) {
                digit *= 2

                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            doubleDigit = !doubleDigit
        }

        return sum % 10 == 0
    }

    private data class NumberToken(
        val digit: Char,
        val corrected: Boolean
    )

    private data class NumberCandidate(
        val value: String,
        val score: Int
    )

    private data class NameCandidate(
        val value: String,
        val index: Int,
        val score: Int
    )
}
