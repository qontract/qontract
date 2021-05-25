package `in`.specmatic.core

import `in`.specmatic.core.pattern.parsedJSON
import `in`.specmatic.core.value.Value
import `in`.specmatic.stubsFrom
import `in`.specmatic.test.TestExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class TestBackwardCompatibilityKtTest {
    @Test
    fun `contract backward compatibility should break when optional key is made mandatory in request`() {
        val gherkin1 = """
Feature: Older contract API

Scenario: api call
Given json Value
| value     | (number) |
| optional? | (number) |
When POST /value
And request-body (Value)
Then status 200
    """.trim()

        val gherkin2 = """
Feature: Newer contract API

Scenario: api call
Given json Value
| value    | (number) |
| optional | (number) |
When POST /value
And request-body (Value)
Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
        assertEquals(1, result.successCount)
    }

    @Test
    fun `contract backward compatibility should break when mandatory key is made optional in response`() {
        val gherkin1 = """
Feature: Older contract API

Scenario: api call
Given json Value
| value     | (number) |
| mandatory | (number) |
When POST /value
And request-body "test"
Then status 200
And response-body (Value)
    """.trim()

        val gherkin2 = """
Feature: Newer contract API

Scenario: api call
Given json Value
| value      | (number) |
| mandatory? | (number) |
When POST /value
And request-body "test"
Then status 200
And response-body (Value)
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
        assertEquals(0, result.successCount)
    }

    @Test
    fun `contract backward compatibility should break when there is value incompatibility one level down`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | address? | (Address?) |
    And type Address
    | street | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type RequestBody
    | address? | (Address?) |
    And type Address
    | street | (number) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should not break when there is optional key compatibility one level down`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | address | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type RequestBody
    | address | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(0, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when optional key is made mandatory one level down in request`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | address | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type RequestBody
    | address | (Address) |
    And type Address
    | street | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when optional key is made mandatory inside an optional parent`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | address? | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type RequestBody
    | address? | (Address) |
    And type Address
    | street | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(2, result.successCount)
        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when optional key is made mandatory inside an optional parent two levels down`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(2, result.successCount)
        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when optional key data type is changed inside an optional parent two levels down`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street | (number) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.successCount)
        assertEquals(2, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when optional value is made mandatory inside an optional parent two levels down`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street? | (string?) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val gherkin2 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type RequestBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body (RequestBody)
    Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(3, result.successCount)
        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when mandatory key is made optional one level down in response`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type ResponseBody
    | address | (Address) |
    And type Address
    | street | (string) |
    When POST /
    And request-body "test"
    Then status 200
    And response-body (ResponseBody)
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type ResponseBody
    | address | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body "test"
    Then status 200
    And response-body (ResponseBody) 
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when mandatory key is made optional two levels down in response`() {
        val gherkin1 = """
Feature: Old contract
  Scenario: Test Scenario
    Given type ResponseBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street | (string) |
    When POST /
    And request-body "test"
    Then status 200
    And response-body (ResponseBody)
    """.trim()

        val gherkin2 = """
Feature: New contract
  Scenario: Test Scenario
    Given type ResponseBody
    | person | (Person) |
    And type Person
    | address? | (Address) |
    And type Address
    | street? | (string) |
    When POST /
    And request-body "test"
    Then status 200
    And response-body (ResponseBody) 
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(1, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should not break when both have an optional keys`() {
        val gherkin1 = """
Feature: API contract

Scenario: api call
Given json Value
| value     | (number) |
| optional? | (number) |
And fact id 10
When POST /value/(id:number)
And request-body (Value)
Then status 200
    """.trim()

        val gherkin2 = """
Feature: API contract

Scenario: api call
Given json Value
| value    | (number) |
| optional? | (number) |
And fact id 10
When POST /value/(id:number)
And request-body (Value)
Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val result: Results = testBackwardCompatibility(olderContract, newerContract)

        println(result.report())

        assertEquals(2, result.successCount)
        assertEquals(0, result.failureCount)
    }

    @Test
    fun `contract backward compatibility should break when a new fact is added`() {
        val gherkin1 = """
Feature: Older contract API

Scenario: api call
Given json Value
| value     | (number) |
| optional? | (number) |
When POST /value/(id:number)
And request-body (Value)
Then status 200
    """.trim()

        val gherkin2 = """
Feature: Older contract API

Scenario: api call
Given json Value
| value    | (number) |
| optional? | (number) |
And fact id 10
When POST /value/(id:number)
And request-body (Value)
Then status 200
    """.trim()

        val olderContract = parseGherkinStringToFeature(gherkin1)
        val newerContract = parseGherkinStringToFeature(gherkin2)

        val results: Results = testBackwardCompatibility(olderContract, newerContract)

        assertEquals(0, results.successCount)
        assertEquals(2, results.failureCount)
    }

    @Test
    fun `contract should test successfully against itself when fact name is specified without a value in the URL path`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
Given fact id
When POST /value/(id:number)
Then status 200
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `contract should test successfully against itself when fact name is specified without a value in the query`() {
        val gherkin = """
Feature: Contract API

Scenario: Test Contract
Given fact id
When GET /value?id=(number)
Then status 200
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(2, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should be able to validate new contract compatibility with optional request body`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
When POST /number
And request-body (number?)
Then status 200
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(2, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should be able to validate new contract compatibility with optional key in request body`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
Given json Number
| number | (number?) |
When POST /number
And request-body (Number)
Then status 200
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(2, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should be able to validate new contract compatibility with optional response body`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
When POST /number
Then status 200
And response-body (number?)
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should be able to validate new contract compatibility with optional key in response body`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
Given json Number
| number | (number?) |
When POST /number
Then status 200
And response-body (Number)
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `contract with a required key should not match a contract with the same key made optional`() {
        val olderBehaviour = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-body (number)
Then status 200
And response-body
| number      | (number) |
| description | (string) |
""".trim())

        val newerBehaviour = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-body (number)
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val results: Results = testBackwardCompatibility(olderBehaviour, newerBehaviour)

        println(results.report())

        assertEquals(0, results.successCount)
        assertEquals(1, results.failureCount)
    }

    @Test
    fun `contract with an optional key in the response should pass against itself`() {
        val behaviour = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-body (number)
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val results: Results = testBackwardCompatibility(behaviour, behaviour)

        println(results.report())
        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should work with multipart content part`() {
        val behaviour = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-part number (number)
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val results: Results = testBackwardCompatibility(behaviour, behaviour)

        println(results.report())
        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should work with multipart file part`() {
        val behaviour = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-part number @number.txt text/plain
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val results: Results = testBackwardCompatibility(behaviour, behaviour)

        println(results.report())
        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `should fail given a file part in one and a content part in the other`() {
        val older = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-part number @number.txt text/plain
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val newer = parseGherkinStringToFeature("""
Feature: Contract API

Scenario: api call
When POST /number
And request-part number (number))
Then status 200
And response-body
| number       | (number) |
| description? | (string) |
""".trim())

        val results: Results = testBackwardCompatibility(older, newer)

        println(results.report())
        assertThat(results.success()).isFalse()
    }

    @Test
    fun `a contract should be backward compatible with itself`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
When POST /number
Then status 200
And response-body (number)
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `a contract with named patterns should be backward compatible with itself`() {
        val gherkin = """
Feature: Contract API

Scenario: api call
Given json Payload
  | number | (number) |
When POST /number
  And request-body (Payload)
Then status 200
And response-body (number)
    """.trim()

        val contract = parseGherkinStringToFeature(gherkin)

        val results: Results = testBackwardCompatibility(contract, contract)

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(1, results.successCount)
        assertEquals(0, results.failureCount)
    }

    @Test
    fun `a contract with named patterns should not be backward compatible with another contract with a different pattern against the same name`() {
        val gherkin1 = """
Feature: Contract API

Scenario: api call
Given json Payload
  | number | (number) |
When POST /number
  And request-body (Payload)
Then status 200
And response-body (number)
    """.trim()

        val gherkin2 = """
Feature: Contract API

Scenario: api call
Given json Payload
  | number | (string) |
When POST /number
  And request-body (Payload)
Then status 200
And response-body (number)
    """.trim()

        val results: Results = testBackwardCompatibility(parseGherkinStringToFeature(gherkin1), parseGherkinStringToFeature(gherkin2))

        if (results.failureCount > 0)
            println(results.report())

        assertEquals(0, results.successCount)
        assertEquals(1, results.failureCount)
    }

    @Test
    fun `a breaking WIP scenario should not break backward compatibility tests`() {
        val gherkin1 = """
Feature: Contract API

@WIP
Scenario: api call
When POST /data
  And request-body (number)
Then status 200
    """.trim()

        val gherkin2 = """
Feature: Contract API

@WIP
Scenario: api call
When POST /data
  And request-body (string)
Then status 200
    """.trim()

        val results: Results = testBackwardCompatibility(parseGherkinStringToFeature(gherkin1), parseGherkinStringToFeature(gherkin2))

        if (results.failureCount > 0)
            println(results.report())

        assertThat(results.successCount).isZero()
        assertThat(results.failureCount).isZero()
        assertThat(results.success()).isTrue()
    }

    @Test
    fun `two xml contracts should be backward compatibility when the only thing changing is namespace prefixes`() {
        val gherkin1 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body <ns1:customer xmlns:ns1="http://example.com/customer"><name>(string)</name></ns1:customer>
Then status 200
    """.trim()

        val gherkin2 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body <ns2:customer xmlns:ns2="http://example.com/customer"><name>(string)</name></ns2:customer>
Then status 200
    """.trim()

        val results: Results = testBackwardCompatibility(parseGherkinStringToFeature(gherkin1), parseGherkinStringToFeature(gherkin2))

        if (results.failureCount > 0)
            println(results.report())

        assertThat(results.successCount).isOne()
        assertThat(results.failureCount).isZero()
        assertThat(results.success()).isTrue()
    }

    @Test
    fun `two xml contracts should not be backward compatibility when optional key is made mandatory in request`() {
        val gherkin1 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body <ns1:customer xmlns:ns1="http://example.com/customer"><name specmatic_occurs="optional">(string)</name></ns1:customer>
Then status 200
    """.trim()

        val gherkin2 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body <ns2:customer xmlns:ns2="http://example.com/customer"><name>(string)</name></ns2:customer>
Then status 200
    """.trim()

        val results: Results = testBackwardCompatibility(parseGherkinStringToFeature(gherkin1), parseGherkinStringToFeature(gherkin2))

        if (results.failureCount > 0)
            println(results.report())

        assertThat(results.successCount).isOne()
        assertThat(results.failureCount).isOne()
        assertThat(results.success()).isFalse()
    }

    @Test
    fun `two xml contracts should not be backward compatibility when mandatory key is made optional in response`() {
        val gherkin1 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body "test"
Then status 200
  And response-body <ns1:customer xmlns:ns1="http://example.com/customer"><name>(string)</name></ns1:customer>
    """.trim()

        val gherkin2 = """
Feature: Contract API

Scenario: api call
When POST /data
  And request-body "test"
Then status 200
  And response-body <ns1:customer xmlns:ns1="http://example.com/customer"><name specmatic_occurs="optional">(string)</name></ns1:customer>
    """.trim()

        val results: Results = testBackwardCompatibility(parseGherkinStringToFeature(gherkin1), parseGherkinStringToFeature(gherkin2))

        if (results.failureCount > 0)
            println(results.report())

        assertThat(results.successCount).isZero()
        assertThat(results.failureCount).isOne()
        assertThat(results.success()).isFalse()
    }

    @Test
    fun `a contract with an optional and a required node in that order should be backward compatible with itself`() {
        val docString = "\"\"\""
        val gherkin =
            """
                Feature: test xml
                    Scenario: Test xml
                    Given type RequestBody
                    $docString
                        <parent>
                            <optionalNode specmatic_occurs="optional" />
                            <requiredNode />
                        </parent>
                    $docString
                    When POST /
                    And request-body (RequestBody)
                    Then status 200
            """.trimIndent()

        val feature = parseGherkinStringToFeature(gherkin)
        val results = testBackwardCompatibility(feature, feature)

        if (results.failureCount > 0)
            println(results.report())

        assertThat(results.success()).isTrue()
    }

    @Test
    fun `removing a key to the request body should not break existing stubs`() {
        val oldContract = """
Feature: User API
  Scenario: Add user
    Given json User
    | name | (string) |
    | address | (string) |
    And json Status
    | status | (string) |
    When POST /user
    And request-body (User)
    Then status 200
    And response-body (Status)
""".trimIndent()

        val newContract = """
Feature: User API
  Scenario: Add user
    Given json User
    | name | (string) |
    And json Status
    | status | (string) |
    When POST /user
    And request-body (User)
    Then status 200
    And response-body (Status)
""".trimIndent()

        stubsFrom(oldContract).doNotBreakWith(newContract)
    }

    val oldContract = """
Feature: User API
  Scenario: Add user
    Given json User
    | name | (string) |
    And json Status
    | status | (string) |
    When POST /user
    And request-body (User)
    Then status 200
    And response-body (Status)
""".trimIndent()

    @Test
    fun `adding a key to the response body should not break existing stubs`() {
        val newContract = """
Feature: User API
  Scenario: Add user
    Given json User
    | name | (string) |
    And json Status
    | status | (string) |
    | data   | (string) |
    When POST /user
    And request-body (User)
    Then status 200
    And response-body (Status)
""".trimIndent()

        stubsFrom(oldContract).doNotBreakWith(newContract)
    }

    @Test
    fun `it should not be possible to avoid stubbing a compulsory query param`() {
        val contract = """
Feature: User API
  Scenario: Get user
    When GET /user?name=(string)
    Then status 200
""".trimIndent()

        val feature = contract.toFeature()
        val response = feature.stubResponse(HttpRequest("GET", path="/user"))
        println(response)
        assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `the contract test should not fail when the service returns an extra key in the response`() {
        val contract = """
Feature: User API
  Scenario: Get user
    Given type User
    | name | (string) |
    When GET /user
    Then status 200
    And response-body (User)
""".trimIndent()

        val feature = contract.toFeature()

        val results: Results = feature.executeTests(object : TestExecutor {
            override fun execute(request: HttpRequest): HttpResponse {
                return HttpResponse.OK(parsedJSON("{\"name\": \"Jane Doe\", \"id\": 10}"))
            }

            override fun setServerState(serverState: Map<String, Value>) {}
        })

        assertThat(results.successCount).isGreaterThan(0)
    }
}
