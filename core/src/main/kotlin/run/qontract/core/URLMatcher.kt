package run.qontract.core

import run.qontract.core.Result.Failure
import run.qontract.core.Result.Success
import run.qontract.core.pattern.*
import run.qontract.core.utilities.URIUtils
import run.qontract.core.value.StringValue
import java.net.URI

const val QUERY_PARAMS_BREADCRUMB = "QUERY-PARAMS"

data class URLMatcher(val queryPattern: Map<String, Pattern>, val pathPattern: List<URLPathPattern>, val path: String) {
    fun matches(uri: URI, sampleQuery: Map<String, String> = emptyMap(), resolver: Resolver = Resolver()): Result {
        val httpRequest = HttpRequest(path = uri.path, queryParams = sampleQuery)
        return matches(httpRequest, resolver)
    }

    private fun matches(httpRequest: HttpRequest, resolver: Resolver): Result {
        return httpRequest to resolver to
                ::matchesPath then
                ::matchesQuery otherwise
                ::handleError toResult
                ::returnResult
    }

    private fun matchesPath(parameters: Pair<HttpRequest, Resolver>): MatchingResult<Pair<HttpRequest, Resolver>> {
        val (httpRequest, resolver) = parameters
        return when(val pathResult = matchesPath(URI(httpRequest.path!!), resolver)) {
            is Failure -> MatchFailure(pathResult.copy(fluff = true))
            else -> MatchSuccess(parameters)
        }
    }

    private fun matchesQuery(parameters: Pair<HttpRequest, Resolver>): MatchingResult<Pair<HttpRequest, Resolver>> {
        val (httpRequest, resolver) = parameters
        return when(val queryResult = matchesQuery(queryPattern, httpRequest.queryParams, resolver)) {
            is Failure -> MatchFailure(queryResult.breadCrumb(QUERY_PARAMS_BREADCRUMB))
            else -> MatchSuccess(parameters)
        }
    }

    private fun matchesPath(uri: URI, resolver: Resolver): Result {
        val pathParts = uri.path.split("/".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

        if (pathPattern.size != pathParts.size)
            return Failure("Expected $uri (having ${pathParts.size} path segments) to match $path (which has ${pathPattern.size} path segments).", breadCrumb = "PATH")

        pathPattern.zip(pathParts).forEach { (urlPathPattern, token) ->
            try {
                val parsedValue = urlPathPattern.tryParse(token, resolver)
                when (val result = resolver.matchesPattern(urlPathPattern.key, urlPathPattern.pattern, parsedValue)) {
                    is Failure -> return when (urlPathPattern.key) {
                        null -> result.breadCrumb("PATH ($uri)")
                        else -> result.breadCrumb("PATH ($uri)").breadCrumb(urlPathPattern.key)
                    }
                }
            } catch (e: ContractException) {
                e.failure().breadCrumb("PATH ($uri)").let {
                    when (urlPathPattern.key) {
                        null -> it
                        else -> it.breadCrumb(urlPathPattern.key)
                    }
                }
            } catch (e: Throwable) {
                Failure(e.localizedMessage).breadCrumb("PATH ($uri)").let {
                    when (urlPathPattern.key) {
                        null -> it
                        else -> it.breadCrumb(urlPathPattern.key)
                    }
                }
            }
        }

        return Success()
    }

    fun generatePath(resolver: Resolver): String {
        return attempt(breadCrumb = "PATH") {
            "/" + pathPattern.mapIndexed { index, urlPathPattern ->
                attempt(breadCrumb = "[$index]") {
                    val key = urlPathPattern.key
                    if (key != null) resolver.generate(key, urlPathPattern.pattern) else urlPathPattern.pattern.generate(resolver)
                }
            }.joinToString("/")
        }
    }

    fun generateQuery(resolver: Resolver): Map<String, String> {
        return attempt(breadCrumb = "QUERY-PARAMS") {
            queryPattern.mapKeys { it.key.removeSuffix("?") }.map { (name, pattern) ->
                attempt(breadCrumb = name) { name to resolver.generate(name, pattern).toString() }
            }.toMap()
        }
    }

    fun newBasedOn(row: Row, resolver: Resolver): List<URLMatcher> {
        val newPathPartsList = newBasedOn(pathPattern.mapIndexed { index, urlPathPattern ->
            val key = urlPathPattern.key

            attempt(breadCrumb = "[$index]") {
                when {
                    key !== null && row.containsField(key) -> {
                        val rowValue = row.getField(key)
                        when {
                            isPatternToken(rowValue) -> attempt("Pattern mismatch in example of path param \"${urlPathPattern.key}\"") {
                                val rowPattern = resolver.getPattern(rowValue)
                                when (val result = urlPathPattern.encompasses(rowPattern, resolver, resolver)) {
                                    is Success -> urlPathPattern.copy(pattern = rowPattern)
                                    else -> throw ContractException(resultReport(result))
                                }
                            }
                            else -> attempt("Format error in example of \"$key\"") { URLPathPattern(ExactValuePattern(urlPathPattern.parse(rowValue, resolver))) }
                        }
                    }
                    else -> urlPathPattern
                }
            }
        }, row, resolver)

        val newURLPathPatternsList = newPathPartsList.map { list -> list.map { it as URLPathPattern } }

        val newQueryParamsList = attempt(breadCrumb = QUERY_PARAMS_BREADCRUMB) {
            val optionalQueryParams = queryPattern

            forEachKeyCombinationIn(optionalQueryParams, row) { entry ->
                newBasedOn(entry.mapKeys { withoutOptionality(it.key) }, row, resolver)
            }
        }

        return newURLPathPatternsList.flatMap { newURLPathPatterns ->
            newQueryParamsList.map { newQueryParams ->
                URLMatcher(newQueryParams, newURLPathPatterns, path)
            }
        }
    }

    override fun toString(): String {
        val url = StringBuilder()
        url.append(path)
        if (queryPattern.isNotEmpty()) url.append("?")
        url.append(queryPattern.mapKeys { it.key.removeSuffix("?") }.map { (key, value) ->
            "$key=$value"
        }.toList().joinToString(separator = "&"))
        return url.toString()
    }
}

internal fun toURLMatcherWithOptionalQueryParams(url: String): URLMatcher = toURLMatcherWithOptionalQueryParams(URI.create(url))

internal fun toURLMatcherWithOptionalQueryParams(urlPattern: URI): URLMatcher {
    val path = urlPattern.path

    val pathPattern = pathToPattern(urlPattern.rawPath)

    val queryPattern = URIUtils.parseQuery(urlPattern.query).mapKeys {
        "${it.key}?"
    }.mapValues {
        if (isPatternToken(it.value))
            DeferredPattern(it.value, it.key)
        else
            ExactValuePattern(StringValue(it.value))
    }

    return URLMatcher(queryPattern = queryPattern, path = path, pathPattern = pathPattern)
}

internal fun toURLMatcher(urlPattern: URI): URLMatcher {
    val path = urlPattern.path

    val pathPattern = pathToPattern(urlPattern.rawPath)

    val queryPattern = URIUtils.parseQuery(urlPattern.query).mapKeys {
        "${it.key}?"
    }.mapValues {
        if (isPatternToken(it.value))
            DeferredPattern(it.value, it.key)
        else
            ExactValuePattern(StringValue(it.value))
    }

    return URLMatcher(queryPattern = queryPattern, path = path, pathPattern = pathPattern)
}

internal fun pathToPattern(rawPath: String): List<URLPathPattern> =
        rawPath.trim('/').split("/").filter { it.isNotEmpty() }.map { part ->
            when {
                isPatternToken(part) -> {
                    val pieces = withoutPatternDelimiters(part).split(":").map { it.trim() }
                    if (pieces.size != 2) {
                        throw ContractException("In path ${rawPath}, $part must be of the format (param_name:type), e.g. (id:number)")
                    }

                    val (name, type) = pieces

                    URLPathPattern(DeferredPattern(withPatternDelimiters(type)), name)
                }
                else -> URLPathPattern(ExactValuePattern(StringValue(part)))
            }
        }

internal fun matchesQuery(queryPattern: Map<String, Pattern>, sampleQuery: Map<String, String>, resolver: Resolver): Result {
    val missingKey = resolver.findMissingKey(queryPattern, sampleQuery.mapValues { StringValue(it.value) }, ::validateUnexpectedKeys)
    if (missingKey != null)
        return missingKeyToResult(missingKey, "query param")

    for (key in queryPattern.keys) {
        val keyName = key.removeSuffix("?")
        if (!sampleQuery.containsKey(keyName)) continue
        try {
            val patternValue = queryPattern.getValue(key)
            val sampleValue = sampleQuery.getValue(keyName)

            val parsedValue = try {
                patternValue.parse(sampleValue, resolver)
            } catch (e: Exception) {
                StringValue(sampleValue)
            }
            when (val result = resolver.matchesPattern(keyName, patternValue, parsedValue)) {
                is Failure -> return result.breadCrumb(keyName)
            }
        } catch (e: ContractException) {
            return e.failure().breadCrumb(keyName)
        } catch (e: Throwable) {
            return Failure(e.localizedMessage).breadCrumb(keyName)
        }
    }
    return Success()
}

