package com.ximenes.products.security.sqli

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.junit.jupiter.api.BeforeEach

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SqliTestBase {

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setupMockMvc(webApplicationContext: WebApplicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    fun checkForInternalErrors(body: String): Boolean {
        val internalErrorTerms = listOf(
            "postgresql", "syntax error", "org.hibernate", "jdbc",
            "at com.", "stacktrace", "information_schema", "pg_catalog"
        )
        return internalErrorTerms.any { body.contains(it, ignoreCase = true) }
    }
}