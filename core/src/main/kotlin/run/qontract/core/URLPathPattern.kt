package run.qontract.core

import run.qontract.core.pattern.*
import run.qontract.core.value.JSONArrayValue
import run.qontract.core.value.NullValue
import run.qontract.core.value.StringValue
import run.qontract.core.value.Value

data class URLPathPattern(override val pattern: Pattern, override val key: String? = null, override val typeAlias: String? = null) : Pattern, Keyed {
    override fun matches(sampleData: Value?, resolver: Resolver): Result =
            resolver.matchesPattern(key, pattern, sampleData ?: NullValue)

    override fun generate(resolver: Resolver): Value =
            if(key != null) resolver.generate(key, pattern) else pattern.generate(resolver)

    override fun newBasedOn(row: Row, resolver: Resolver): List<URLPathPattern> =
            pattern.newBasedOn(row, resolver).map { URLPathPattern(it, key) }

    override fun parse(value: String, resolver: Resolver): Value = pattern.parse(value, resolver)

    override fun encompasses(otherPattern: Pattern, thisResolver: Resolver, otherResolver: Resolver, typeStack: TypeStack): Result {
        if(otherPattern !is URLPathPattern)
            return Result.Failure("Expected url type, got ${otherPattern.typeName}")

        return otherPattern.pattern.fitsWithin(patternSet(thisResolver), otherResolver, thisResolver, typeStack)
    }

    override fun listOf(valueList: List<Value>, resolver: Resolver): Value {
        return JSONArrayValue(valueList)
    }

    fun tryParse(token: String, resolver: Resolver): Value {
        return try {
            this.pattern.parse(token, resolver)
        } catch (e: Throwable) {
            if (isPatternToken(token) && token.contains(":"))
                StringValue(withPatternDelimiters(withoutPatternDelimiters(token).split(":".toRegex(), 2)[1]))
            else
                StringValue(token)
        }
    }

    override val typeName: String = "url path"
}