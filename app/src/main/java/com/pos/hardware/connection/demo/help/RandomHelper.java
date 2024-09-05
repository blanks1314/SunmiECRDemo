package com.pos.hardware.connection.demo.help;

import java.util.Random;

public class RandomHelper {

    public static String randomString(int length) {
        String string = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            String temp = random.nextInt(2) % 2 == 0 ? "char" : "num";
            boolean b1 = "num".equalsIgnoreCase(temp);
            boolean b2 = "char".equalsIgnoreCase(temp);
            if (b1) {
                string += String.valueOf(random.nextInt(10));
            }
            if (b2) {
                int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
                int index = choice + random.nextInt(26);
                string += (char) index;
            }
        }
        return string;
    }

}