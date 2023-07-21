package ru.rest.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.rest.telegram.config.BotSettings;
import ru.rest.telegram.model.ExamUser;
import ru.rest.telegram.model.ExaminationTicket;
import ru.rest.telegram.model.Ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Bot extends TelegramLongPollingBot {
    private final static BotSettings botSettings = BotSettings.getInstance();
    private static Bot instance;
    private final TelegramBotsApi telegramBotsApi;
    private final Map<Long, ExamUser> examUserMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("здесь");
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        Update update = updates.get(0);
        Message message = null;
        ExamUser examUser = null;
        if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
            examUser = getExamUser(message);
            generateResponseFromQuestion(update, examUser);
            return;
        }
        if (update.getMessage() != null) {
            message = update.getMessage();
            examUser = getExamUser(message);
        }
//        if(examUser.getAdmin().equals("1")){
//            if (message.hasText()) {
//                sendMsg(examUser.getChatId(),"Ведите вопрос");
//            }
//        }
        if (examUser.getLastTicket() != null) {
            sendMsg(examUser.getChatId(), "Меню заблокировано");
            return;
        }

        if (message.hasText()) {
            switch (message.getText()) {
                case "Помощь":
                    sendMsg(message.getChatId(), "Этот бот помогает готовиться к собеседованиям");
                    break;
                case "Мой ID":
                    sendMsg(message.getChatId(), message.getChatId().toString());
                    break;
                case "О программе":
                    sendMsg(message.getChatId(), "Эта программа создана для автоматизации управления за Raspberry Pi");
                    break;
                case "Java тесты":
                    sendMsg(message.getChatId(), "Запускаю тест, у Вас есть 30 минут");
                    generateTestForExamUser(examUser, "SQL");
                    break;
                case "admin":
                    sendMsg(message.getChatId(), "Режим админа включен");
                    examUser.setAdmin("1");
                    break;
                default:
                    sendMsg(message.getChatId(), "Команда не распознана");

            }
        }
    }

    private void generateResponseFromQuestion(Update update, ExamUser examUser) {
        ExaminationTicket examinationTicket = examUser.getExaminationTickets();
        Ticket lastTicket = examUser.getLastTicket();
        int numberOfAnswer = Integer.parseInt(update.getCallbackQuery().getData());
        lastTicket.setActualAnswer(numberOfAnswer);
        sendMsg(examUser.getChatId(), "Ваш ответ: " + lastTicket.getAnswers().get(numberOfAnswer - 1));
        examinationTicket.getTicketsPassed().add(lastTicket);
        Ticket newFromUser = examinationTicket.getTicketsActual().remove(0);
        if (examinationTicket.getTicketsActual().size() > 0) {
            examUser.setLastTicket(newFromUser);
            sendMsgWithInlineKeyBoard(examUser.getChatId(), generateTextForTicket(examUser.getLastTicket(), examinationTicket.getTicketsPassed().size()));
        } else {
            sendMsg(examUser.getChatId(), " последний вопрос тесты закончены");
            sendMsg(examUser.getChatId(), generateResult(examUser));
            sendMsg(969507756L, "@" + examUser.getName() + " " + generateResult(examUser));
        }
    }

    private String generateResult(ExamUser examUser) {
        examUser.setLastTicket(null);
        int allTickets = examUser.getExaminationTickets().getTicketsPassed().size();
        int rightAnswerCount = examUser.getExaminationTickets().getTicketsPassed().stream()
                .mapToInt(value -> value.getCorrectAnswer().equals(value.getActualAnswer()) ? 1 : 0).sum();
        return "Правильных ответов " + rightAnswerCount * 100 / allTickets + " %";
    }

    private void generateTestForExamUser(ExamUser examUser, String type) {
        ExaminationTicket examinationTicket = new ExaminationTicket();
        ArrayList<Ticket> tickets = MicroServiceUtil.getExaminationTicketFromMicroservice(type, 10);
        examUser.setLastTicket(tickets.remove(0));
        examinationTicket.setTicketsActual(tickets);
        examinationTicket.setStartTime(Instant.now());
        examUser.setExaminationTickets(examinationTicket);
        examUser.setExaminationStartTime(Instant.now());
        String textForSend = generateTextForTicket(examUser.getLastTicket(), examinationTicket.getTicketsPassed().size());
        sendMsgWithInlineKeyBoard(examUser.getChatId(), textForSend);

    }

    private void sendMsgWithInlineKeyBoard(Long chatId, String textForSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(String.valueOf(chatId));  //sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(textForSend);
        try {
            sendMessage.setReplyMarkup(new ReplyKeyboardMarkup());
            setButtonsOnArea(sendMessage);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String generateTextForTicket(Ticket ticket, int size) {
        int num = size + 1;
        return "Вопрос " + num + ": " + ticket.getTitle() + "\n\n" +
                "A: " + ticket.getAnswers().get(0) + "\n\n" +
                "B: " + ticket.getAnswers().get(1) + "\n\n" +
                "C: " + ticket.getAnswers().get(2) + "\n\n" +
                "D: " + ticket.getAnswers().get(3) + "\n\n";
    }

    private ExamUser getExamUser(Message message) {
        ExamUser examUser;
        if (examUserMap.containsKey(message.getChatId())) {
            examUser = examUserMap.get(message.getChatId());
        } else {
            examUser = new ExamUser();
            examUser.setChatId(message.getChatId());
            examUserMap.put(message.getChatId(), examUser);
        }
        examUser.setName(message.getChat().getUserName());
        return examUser;
    }


    public void sendMsg(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(String.valueOf(chatId));  //sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            setButtons(sendMessage);
            //setButtonsOnArea(sendMessage);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Помощь"));
        keyboardFirstRow.add(new KeyboardButton("Java тесты"));
        keyboardSecondRow.add(new KeyboardButton("Мой ID"));
        keyboardSecondRow.add(new KeyboardButton("О программе"));

        keyboardRowList.add(keyboardFirstRow);
        keyboardRowList.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    public void setButtonsOnArea(SendMessage sendMessage) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonA = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonB = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonC = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonD = new InlineKeyboardButton();
        inlineKeyboardButtonA.setText("A");
        inlineKeyboardButtonA.setCallbackData("1");
        inlineKeyboardButtonB.setText("B");
        inlineKeyboardButtonB.setCallbackData("2");
        inlineKeyboardButtonC.setText("C");
        inlineKeyboardButtonC.setCallbackData("3");
        inlineKeyboardButtonD.setText("D");
        inlineKeyboardButtonD.setCallbackData("4");
        List<InlineKeyboardButton> keyboardButtonsRowTop = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRowDown = new ArrayList<>();
        keyboardButtonsRowTop.add(inlineKeyboardButtonA);
        keyboardButtonsRowTop.add(inlineKeyboardButtonB);
        keyboardButtonsRowDown.add(inlineKeyboardButtonC);
        keyboardButtonsRowDown.add(inlineKeyboardButtonD);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRowTop);
        rowList.add(keyboardButtonsRowDown);
        inlineKeyboardMarkup.setKeyboard(rowList);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
    }


    public void registerBot() {
        try {
            telegramBotsApi.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API initialization error: " + e.getMessage());
        }
    }

    {
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            registerBot();
            //registerCommands();
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram ru.rest.telegram.service.Bot initialization error: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Aston_Akrafit_bot";
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public String getBotToken() {
        return botSettings.getToken();
    }

    public static Bot getInstance() {
        if (instance == null)
            instance = new Bot();
        //examUserMap = new HashMap<>();
        return instance;
    }
//    private void registerCommands() {
//        register(new CommandStart());
//        register(new CommandHelp());
//        setRegisteredCommands();
//    }
//    private void setRegisteredCommands() {
//        registeredCommands = getRegisteredCommands()
//                .stream()
//                .map(IBotCommand::getCommandIdentifier)
//                .collect(Collectors.toList());
//    }
}
//        if (update.hasMessage() && update.getMessage().hasPhoto())  {
//            long chat_id = update.getMessage().getChatId();
//            List<PhotoSize> photos = update.getMessage().getPhoto();
//            String f_id = photos.stream()
//                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
//                    .findFirst()
//                    .orElse(null).getFileId();
//download
//            String number = new Util().getPropertyValue("number");
//            try {
//                //if we have on my.properties number we write file name
//                if(!number.equals("0")){
//                    uploadFile(number+".jpg", f_id);
//                }else {
//                    uploadFile("file.jpg", f_id);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
// Set photo caption
//            String caption;
//            if(!number.equals("0")){
//                caption= number+".jpg сохранен в базе";
//            }else {
//                caption = "Фото без номера, введите n номер";
//            }
//            SendPhoto msg = new SendPhoto()
//                    .setChatId(chat_id)
//                    .setPhoto(f_id)
//                    .setCaption(caption);
//            try {
//                this.execute(msg);
//            } catch (TelegramApiException e) {
//                e.printStackTrace();
//            }
//        }

//    public void sendMsg(String text){
//        SendMessage sendMessage = new SendMessage().setChatId("969507756").setText(text);
//        try {
//            execute(sendMessage);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }

//                    if(message.getText().matches("^time\\s\\d+")){
//                        String time = message.getText();
//                        time = time.replace("time ","");
//                        try {
//                            new Util().setPropertyValue("time", time);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if(message.getText().matches("^n\\s\\d+")){
//                        String number = message.getText();
//                        number = number.replace("n ","");
//                        new Util().renameImg("../img/file.jpg","../img/" + number + ".jpg");
//                        try {
//                            new Util().setPropertyValue("number","0");
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if(message.getText().matches("^w\\s\\d+")){
//                        String txt = message.getText();
//                        txt = txt.replace("w ","");
//                        new Util().writer(txt);
//                    }