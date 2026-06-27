package com.ecommerce.authdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LanguageController {

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/translations")
    public Map<String,String> getTranslations(
            @RequestParam(defaultValue = "en") String lang){

        Locale locale = new Locale(lang);

        Map<String,String> data = new HashMap<>();

        // Home Screen
        data.put("welcome", messageSource.getMessage("welcome",null,locale));
        data.put("home", messageSource.getMessage("home",null,locale));

        // Login Screen
        data.put("login", messageSource.getMessage("login",null,locale));

        // Common Buttons
        data.put("skip", messageSource.getMessage("skip",null,locale));

        // Age Selection Screen
        data.put("age_0_18", messageSource.getMessage("age.0_18",null,locale));
        data.put("age_19_24", messageSource.getMessage("age.19_24",null,locale));
        data.put("age_25_40", messageSource.getMessage("age.25_40",null,locale));
        data.put("age_40_plus", messageSource.getMessage("age.40_plus",null,locale));

        // Profile Screen
        data.put("profile", messageSource.getMessage("profile",null,locale));
        data.put("logout", messageSource.getMessage("logout",null,locale));

        data.put("welcome_back", messageSource.getMessage("welcome_back",null,locale));
        data.put("sign_in", messageSource.getMessage("sign_in",null,locale));
        data.put("email_or_mobile", messageSource.getMessage("email_or_mobile",null,locale));
        data.put("terms_text", messageSource.getMessage("terms_text",null,locale));
        data.put("sign_in_continue", messageSource.getMessage("sign_in_continue",null,locale));
        data.put("create_account", messageSource.getMessage("create_account",null,locale));
        data.put("continue_google", messageSource.getMessage("continue_google",null,locale));
        data.put("new_user", messageSource.getMessage("new_user",null,locale));

        return data;
    }
}