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
package com.github.epadronu.balin.core
/* ***************************************************************************/

/* ***************************************************************************/
import com.github.epadronu.balin.config.Configuration
import com.github.epadronu.balin.config.ConfigurationBuilder
import com.github.epadronu.balin.config.ConfigurationSetup
import com.github.epadronu.balin.exceptions.MissingPageUrlException
import com.github.epadronu.balin.exceptions.PageImplicitAtVerificationException
import org.openqa.selenium.Alert
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent
import kotlin.reflect.full.primaryConstructor
/* ***************************************************************************/

/* ***************************************************************************/
/**
 * Balin's backbone. The `Browser` interface binds together the different
 * abstractions that form part of the library.
 *
 * Additionally, this interface defines the entry point for the Domain-Specific
 * Language which Balin is built around.
 */
interface Browser : JavaScriptSupport, WaitingSupport, WebDriver {

    companion object {
        /**
         * The builder in charge of generating the configuration.
         */
        private val configurationBuilder: ConfigurationBuilder = ConfigurationBuilder()

        /**
         * The name of the property that dictates which setup to use.
         */
        internal const val BALIN_SETUP_NAME_PROPERTY: String = "balin.setup.name"

        /**
         * Retrieves the configuration setup, taking in account the value of
         * the `balin.setup.name` property.
         */
        val desiredConfiguration: ConfigurationSetup
            get() = configurationBuilder.build().run {
                setups[System.getProperty(BALIN_SETUP_NAME_PROPERTY) ?: "default"] ?: this
            }

        /**
         * Domain-Specific language that let's you configure Balin's global
         * behavior.
         *
         * @sample com.github.epadronu.balin.config.ConfigurationTests.call_the_configure_method_and_make_changes
         *
         * @param block here you can interact with the DSL.
         */
        fun configure(block: ConfigurationBuilder.() -> Unit) {
            block(configurationBuilder)
        }

        /**
         * This method represents the entry point for the Domain-Specific
         * Language which Balin is built around.
         *
         * `drive` is the main abstraction layer for Selenium-WebDriver. Inside
         * the [block] it receives as parameter, you can interact with the
         * driver and use all the features Balin has to offer.
         *
         * @sample com.github.epadronu.balin.core.BrowserTests.perform_a_simple_web_navigation
         *
         * @param driverFactory provides the driver on which the navigation and interactions will be performed.
         * @param autoQuit indicates if the driver should quit at the end of the [block].
         * @param block here you interact with the driver alongside of Balin's assistance.
         */
        fun drive(
            driverFactory: () -> WebDriver = desiredConfiguration.driverFactory,
            autoQuit: Boolean = desiredConfiguration.autoQuit,
            block: Browser.() -> Unit) = drive(Configuration(autoQuit, driverFactory), block)

        /**
         * This method represents the entry point for the Domain-Specific
         * Language which Balin is built around.
         *
         * `drive` is the main abstraction layer for Selenium-WebDriver. Inside
         * the [block] it receives as parameter, you can interact with the
         * driver and use all the features Balin has to offer.
         *
         * @sample com.github.epadronu.balin.core.BrowserTests.perform_a_simple_web_navigation
         *
         * @param configuration defines Balin's local behavior for [block] only.
         * @param block here you interact with the driver alongside of Balin's assistance.
         */
        fun drive(configuration: Configuration, block: Browser.() -> Unit) {
            val desiredConfiguration = configuration.run {
                setups[System.getProperty(BALIN_SETUP_NAME_PROPERTY) ?: "default"] ?: this
            }

            BrowserImpl(desiredConfiguration).apply {
                try {
                    block()
                } catch (throwable: Throwable) {
                    throw throwable
                } finally {
                    if (configurationSetup.autoQuit) {
                        quit()
                    }
                }
            }
        }
    }

    /**
     * Tells the browser at what page it should be located.
     *
     * If the page defines an _implicit at verification_, then it will be
     * invoked immediately. If such verification fails, Balin will throw a
     * [PageImplicitAtVerificationException] in order to perform an early
     * failure.
     *
     * @sample com.github.epadronu.balin.core.BrowserTests.model_a_page_into_a_Page_Object_and_interact_with_it_via_the_at_method
     *
     * @param T the page's type.
     * @param factory provides an instance of the page given the driver being used by the browser.
     * @Returns An instance of the current page.
     * @throws PageImplicitAtVerificationException if the page has an _implicit at verification_ which have failed.
     */
    fun <T : Page> at(factory: (Browser) -> T): T = factory(this).apply {
        if (!verifyAt()) {
            throw PageImplicitAtVerificationException()
        }
    }

    /**
     * Navigates to the given page.
     *
     * If the page has not defined a URL, then a
     * [MissingPageUrlException] will be thrown immediately since
     * is not possible to perform the navigation.
     *
     * If the page defines an _implicit at verification_, then it
     * will be invoked immediately. If such verification fails, Balin
     * will throw a [PageImplicitAtVerificationException] in order to
     * perform an early failure.
     *
     * @sample com.github.epadronu.balin.core.BrowserTests.perform_a_simple_web_navigation
     *
     * @param T the page's type.
     * @param factory provides an instance of the page given the driver being used by the browser.
     * @Returns An instance of the current page.
     * @throws MissingPageUrlException if the page has not defined a URL.
     * @throws PageImplicitAtVerificationException if the page has an _implicit at verification_ which have failed.
     * @see org.openqa.selenium.WebDriver.get
     */
    fun <T : Page> to(factory: (Browser) -> T): T = factory(this).apply {
        get(url ?: throw MissingPageUrlException())

        if (!verifyAt()) {
            throw PageImplicitAtVerificationException()
        }
    }

    /**
     * Navigates to the given URL.
     *
     * @param url the URL the browser will navigate to.
     * @return The browser's current URL.
     *
     * @see org.openqa.selenium.WebDriver.get
     */
    fun to(url: String): String {
        get(url)

        return currentUrl
    }
}
/* ***************************************************************************/

/* ***************************************************************************/
/**
 * Switches to the currently active modal dialog for this particular driver instance.
 *
 * You can interact with the dialog handler only inside [alertContext].
 *
 * @sample com.github.epadronu.balin.core.WithAlertTests.validate_context_switching_to_and_from_an_alert_popup_and_accept_it
 *
 * @param alertContext here you can interact with the dialog handler.
 * @throws org.openqa.selenium.NoAlertPresentException If the dialog cannot be found.
 */
inline fun Browser.withAlert(alertContext: Alert.() -> Unit): Unit = try {
    switchTo().alert().run {
        alertContext()

        if (this == alertIsPresent().apply(driver)) {
            dismiss()
        }
    }
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame by its (zero-based) index and switch the driver's context to
 * it.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame till the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_index
 *
 * @param index (zero-based) index.
 * @param iFrameContext here you can interact with the given IFrame.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun Browser.withFrame(index: Int, iFrameContext: () -> Unit): Unit = try {
    switchTo().frame(index)
    iFrameContext()
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame by its name or ID. Frames located by matching name attributes
 * are always given precedence over those matched by ID.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame till the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_id
 *
 * @param nameOrId the name of the frame window, the id of the &lt;frame&gt; or &lt;iframe&gt; element, or the (zero-based) index.
 * @param iFrameContext here you can interact with the given IFrame.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun Browser.withFrame(nameOrId: String, iFrameContext: () -> Unit): Unit = try {
    switchTo().frame(nameOrId)
    iFrameContext()
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame using its previously located WebElement.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame till the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_web_element
 *
 * @param webElement the frame element to switch to.
 * @param iFrameContext here you can interact with the given IFrame.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun Browser.withFrame(webElement: WebElement, iFrameContext: () -> Unit): Unit = try {
    switchTo().frame(webElement)
    iFrameContext()
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame by its (zero-based) index and switch the driver's context to
 * it.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame via a `Page Object` of type [T] till
 * the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_index_and_pages
 *
 * @param T the `Page Object`'s type.
 * @param index (zero-based) index.
 * @param iFrameContext here you can interact with the given IFrame via a `Page Object`.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun <reified T : Page> Browser.withFrame(index: Int, iFrameContext: T.() -> Unit): Unit = try {
    switchTo().frame(index)
    @Suppress("UNCHECKED_CAST")
    iFrameContext(at(T::class.primaryConstructor as (Browser) -> T))
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame by its name or ID. Frames located by matching name attributes
 * are always given precedence over those matched by ID.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame via a `Page Object` of type [T] till
 * the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_id_and_pages
 *
 * @param T the `Page Object`'s type.
 * @param nameOrId the name of the frame window, the id of the &lt;frame&gt; or &lt;iframe&gt; element, or the (zero-based) index.
 * @param iFrameContext here you can interact with the given IFrame via a `Page Object`.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun <reified T : Page> Browser.withFrame(nameOrId: String, iFrameContext: T.() -> Unit): Unit = try {
    switchTo().frame(nameOrId)
    @Suppress("UNCHECKED_CAST")
    iFrameContext(at(T::class.primaryConstructor as (Browser) -> T))
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Select a frame using its previously located WebElement.
 *
 * Once the frame has been selected, all subsequent calls on the WebDriver
 * interface are made to that frame via a `Page Object` of type [T] till
 * the end of [iFrameContext].
 *
 * If a exception is thrown inside [iFrameContext], the driver will return to
 * its default context.
 *
 * @sample com.github.epadronu.balin.core.WithFrameTests.validate_context_switching_to_and_from_an_iframe_with_web_element_and_pages
 *
 * @param T the `Page Object`'s type.
 * @param webElement the frame element to switch to.
 * @param iFrameContext here you can interact with the given IFrame via a `Page Object`.
 * @throws org.openqa.selenium.NoSuchFrameException If the frame cannot be found.
 */
inline fun <reified T : Page> Browser.withFrame(webElement: WebElement, iFrameContext: T.() -> Unit): Unit = try {
    switchTo().frame(webElement)
    @Suppress("UNCHECKED_CAST")
    iFrameContext(at(T::class.primaryConstructor as (Browser) -> T))
} catch (throwable: Throwable) {
    throw throwable
} finally {
    switchTo().defaultContent()
}

/**
 * Switch the focus of future commands for this driver to the window with the
 * given name/handle.
 *
 * The name/handle can be omitted and the switching will be performed
 * automatically if and only if there is only two windows currently
 * opened.
 *
 * Once the window has been selected, all subsequent calls on the WebDriver
 * interface are made to that window till the end of [windowContext].
 *
 * If a exception is thrown inside [windowContext], the driver will return to
 * the previous window.
 *
 * @sample com.github.epadronu.balin.core.WithWindowTests.validate_context_switching_to_and_from_a_window
 *
 * @param nameOrHandle The name of the window or the handle as returned by [WebDriver.getWindowHandle]
 * @param windowContext Here you can interact with the given window.
 * @throws NoSuchWindowException If the window cannot be found or, in the case of no name or handle is indicated,
 *                               there is not exactly two windows currently opened.
 */
inline fun Browser.withWindow(nameOrHandle: String? = null, windowContext: WebDriver.() -> Unit) {
    val originalWindow = windowHandle

    val targetWindow = nameOrHandle ?: windowHandles.toSet().minus(originalWindow).run {
        when (size) {
            0 -> throw NoSuchWindowException("No new window was found")
            1 -> first()
            else -> throw NoSuchWindowException("The window cannot be determined automatically")
        }
    }

    try {
        switchTo().window(targetWindow).windowContext()
    } catch (throwable: Throwable) {
        throw throwable
    } finally {
        if (originalWindow != targetWindow && windowHandles.contains(targetWindow)) {
            close()
        }

        switchTo().window(originalWindow)
    }
}
/* ***************************************************************************/
