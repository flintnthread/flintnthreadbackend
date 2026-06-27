package com.ecommerce.authdemo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

    public class DateUtil {

        public static String format(LocalDateTime dateTime) {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return dateTime.format(formatter);
        }

    }

