package utils;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

public class EmailService {

    // CONSEIL : ne mettez JAMAIS la vraie clé API en dur dans le code.
    // Utilisez une variable d'environnement SENDGRID_API_KEY (cf. doc dans la réponse).
    private static final String SENDGRID_API_KEY = "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";

    // Adresse d'expéditeur (celle validée chez SendGrid)
    private static final String FROM_EMAIL = "lamma.service@gmail.com";
    private static final String FROM_NAME = "LAMMA";

    public static void sendWelcomeEmail(String toEmail, String sponsorName) {
        if (SENDGRID_API_KEY == null || SENDGRID_API_KEY.isBlank()) {
            System.err.println("SENDGRID_API_KEY non défini. E-mail non envoyé.");
            return;
        }

        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            String subject = "Bienvenue parmi nos sponsors";

            String body = "Bonjour " + sponsorName + ",\n\n" +
                    "Merci d'avoir rejoint les sponsors de notre plateforme LAMMA.\n" +
                    "Nous sommes ravis de vous compter parmi nos partenaires.\n\n" +
                    "Cordialement,\nL'équipe LAMMA";

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(SENDGRID_API_KEY);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println("Email SendGrid status: " + response.getStatusCode());
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail: " + e.getMessage());
        }
    }
}

