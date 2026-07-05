package ru.cheftable.web.page

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.cheftable.application.auth.AuthService
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.auth.UnauthorizedException

@Controller
class AuthPageController(private val authService: AuthService, @Value("\${chef-table.auth.secure-cookie:false}") private val secureCookie: Boolean) {
    @GetMapping("/")
    fun home(@AuthenticationPrincipal client: AuthenticatedClient?) = if (client == null) "redirect:/login" else "redirect:/schedule"

    @GetMapping("/login")
    fun login(@RequestParam(required = false) redirect: String?, model: Model): String {
        if (!model.containsAttribute("form")) model.addAttribute("form", PhoneForm(redirect = safeRedirect(redirect)))
        return "auth/phone"
    }

    @PostMapping("/login")
    fun requestCode(@Valid @ModelAttribute("form") form: PhoneForm, bindingResult: BindingResult, model: Model): String {
        if (bindingResult.hasErrors()) return "auth/phone"
        val result = authService.requestSms(form.phone)
        model.addAttribute("phone", form.phone)
        model.addAttribute("form", CodeForm(form.phone, redirect = safeRedirect(form.redirect)))
        model.addAttribute("expiresInSeconds", result.expiresInSeconds)
        model.addAttribute("resendAvailableInSeconds", result.resendAvailableInSeconds)
        return "auth/code"
    }

    @GetMapping("/login/code")
    fun code(@RequestParam phone: String, @RequestParam(required = false) redirect: String?, model: Model): String {
        model.addAttribute("phone", phone)
        model.addAttribute("form", CodeForm(phone, redirect = safeRedirect(redirect)))
        model.addAttribute("expiresInSeconds", 300)
        model.addAttribute("resendAvailableInSeconds", 60)
        return "auth/code"
    }

    @PostMapping("/login/code")
    fun verifyCode(@Valid @ModelAttribute("form") form: CodeForm, bindingResult: BindingResult, model: Model, response: HttpServletResponse): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("phone", form.phone)
            return "auth/code"
        }
        val result = try {
            authService.verifySms(form.phone, form.code)
        } catch (ex: UnauthorizedException) {
            bindingResult.rejectValue("code", "invalid", "Неверный или истекший код")
            model.addAttribute("phone", form.phone)
            return "auth/code"
        }
        response.addCookie(authCookie(result.accessToken, 30 * 24 * 60 * 60))
        return "redirect:${safeRedirect(form.redirect) ?: "/schedule"}"
    }

    @PostMapping("/logout")
    fun logout(@org.springframework.web.bind.annotation.CookieValue(name = AUTH_COOKIE_NAME, required = false) token: String?, response: HttpServletResponse): String {
        if (!token.isNullOrBlank()) authService.logout(token)
        response.addCookie(authCookie("", 0))
        return "redirect:/login?logout"
    }

    private fun safeRedirect(value: String?): String? = value?.takeIf { it.startsWith("/") && !it.startsWith("//") }

    private fun authCookie(value: String, maxAge: Int): Cookie = Cookie(AUTH_COOKIE_NAME, value).apply {
        isHttpOnly = true
        secure = secureCookie
        path = "/"
        this.maxAge = maxAge
        setAttribute("SameSite", "Lax")
    }
}

const val AUTH_COOKIE_NAME = "CHEF_TABLE_SESSION"

data class PhoneForm(@field:Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Введите телефон в международном формате") var phone: String = "", var redirect: String? = null)
data class CodeForm(
    @field:Pattern(regexp = "^\\+?[0-9]{10,15}$") var phone: String = "",
    @field:Pattern(regexp = "^\\d{4}$", message = "Код состоит из 4 цифр") var code: String = "",
    var redirect: String? = null,
)
