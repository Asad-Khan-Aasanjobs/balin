/******************************************************************************
 * Copyright 2016 Edinson E. Padrón Urdaneta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

/* ***************************************************************************/
package com.github.epadronu.balin.config
/* ***************************************************************************/

/* ***************************************************************************/
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.github.epadronu.balin.core.Browser
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
/* ***************************************************************************/

/* ***************************************************************************/
class ConfigurationTests {

    private val testDriverFactory = { HtmlUnitDriver(BrowserVersion.FIREFOX_52) }

    @AfterMethod
    fun cleanup() {
        Browser.configure {
            autoQuit = ConfigurationSetup.Default.autoQuit

            driverFactory = ConfigurationSetup.Default.driverFactory

            setups = mapOf()
        }

        System.clearProperty(Browser.BALIN_SETUP_NAME_PROPERTY)
    }

    @Test
    fun `Use the default configuration`() {
        Assert.assertEquals(Browser.desiredConfiguration, ConfigurationSetup.Default)
    }

    @Test
    fun `Call the configure method but don't modify a thing`() {
        Browser.configure { }

        Assert.assertEquals(Browser.desiredConfiguration, ConfigurationSetup.Default)
    }

    @Test
    fun `Call the configure method and make changes`() {
        val desiredConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.configure {
            autoQuit = desiredConfigurationSetup.autoQuit

            driverFactory = desiredConfigurationSetup.driverFactory
        }

        Assert.assertEquals(Browser.desiredConfiguration, desiredConfigurationSetup)
    }

    @Test
    fun `Call the configure method with a default setup and use it implicitly`() {
        val defaultConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.configure {
            setups = mapOf(
                "default" to defaultConfigurationSetup
            )
        }

        Assert.assertEquals(Browser.desiredConfiguration, defaultConfigurationSetup)
    }

    @Test
    fun `Call the configure method with a default setup and use it explicitly`() {
        val defaultConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.configure {
            setups = mapOf(
                "default" to defaultConfigurationSetup
            )
        }

        System.setProperty(Browser.BALIN_SETUP_NAME_PROPERTY, "default")

        Assert.assertEquals(Browser.desiredConfiguration, defaultConfigurationSetup)
    }

    @Test
    fun `Call the configure method with a development setup and don't use it`() {
        val developmentConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.configure {
            setups = mapOf(
                "development" to developmentConfigurationSetup
            )
        }

        Assert.assertNotEquals(Browser.desiredConfiguration, developmentConfigurationSetup)
    }

    @Test
    fun `Call the configure method with a development setup and use it`() {
        val developmentConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.configure {
            setups = mapOf(
                "development" to developmentConfigurationSetup
            )
        }

        System.setProperty(Browser.BALIN_SETUP_NAME_PROPERTY, "development")

        Assert.assertEquals(Browser.desiredConfiguration, developmentConfigurationSetup)
    }

    @Test
    fun `Call the drive method with a desired configuration`() {
        val desiredConfigurationSetup = Configuration(false, testDriverFactory)

        Browser.drive(desiredConfigurationSetup) {
            Assert.assertEquals(configurationSetup, desiredConfigurationSetup)
        }
    }

    @Test
    fun `Call the drive method with a default-setup configuration and use it implicitly`() {
        val defaultConfigurationSetup = Configuration(false, testDriverFactory)

        val desiredConfigurationSetup = ConfigurationBuilder().apply {
            driverFactory = testDriverFactory

            setups = mapOf(
                "default" to defaultConfigurationSetup
            )
        }.build()

        Browser.drive(desiredConfigurationSetup) {
            Assert.assertEquals(configurationSetup, defaultConfigurationSetup)
        }
    }

    @Test
    fun `Call the drive method with a default-setup configuration and use it explicitly`() {
        val defaultConfigurationSetup = Configuration(false, testDriverFactory)

        val desiredConfigurationSetup = ConfigurationBuilder().apply {
            driverFactory = testDriverFactory

            setups = mapOf(
                "default" to defaultConfigurationSetup
            )
        }.build()

        System.setProperty(Browser.BALIN_SETUP_NAME_PROPERTY, "default")

        Browser.drive(desiredConfigurationSetup) {
            Assert.assertEquals(configurationSetup, defaultConfigurationSetup)
        }
    }

    @Test
    fun `Call the drive method with a development-setup configuration and don't use it`() {
        val developmentConfigurationSetup = Configuration(false, testDriverFactory)

        val desiredConfigurationSetup = ConfigurationBuilder().apply {
            driverFactory = testDriverFactory

            setups = mapOf(
                "development" to developmentConfigurationSetup
            )
        }.build()

        Browser.drive(desiredConfigurationSetup) {
            Assert.assertEquals(configurationSetup, desiredConfigurationSetup)
        }
    }

    @Test
    fun `Call the drive method with a development-setup configuration and use it`() {
        val developmentConfigurationSetup = Configuration(false, testDriverFactory)

        val desiredConfigurationSetup = ConfigurationBuilder().apply {
            driverFactory = testDriverFactory

            setups = mapOf(
                "development" to developmentConfigurationSetup
            )
        }.build()

        System.setProperty(Browser.BALIN_SETUP_NAME_PROPERTY, "development")

        Browser.drive(desiredConfigurationSetup) {
            Assert.assertEquals(configurationSetup, developmentConfigurationSetup)
        }
    }
}
/* ***************************************************************************/
