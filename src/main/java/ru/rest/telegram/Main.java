package ru.rest.telegram;

import ru.rest.telegram.service.Bot;

public class Main {
    public static void main(String[] args) {
        Bot bot = Bot.getInstance();
        System.out.println("Telegram bot started");
    }
}
