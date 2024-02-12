package uz.tuit.hrsystem.service;

import freemarker.template.Configuration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.tuit.hrsystem.payload.ApiResponse;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender sender;
    private final Configuration configuration;

    private final static Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    @Async
    public ApiResponse sendText(String sendToEmail, String text) {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setText("Sizning kodingiz: " + text);
            simpleMailMessage.setTo(sendToEmail);
            simpleMailMessage.setSubject("From Lom MCHJ");
            sender.send(simpleMailMessage);
            return new ApiResponse("Success", true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

}