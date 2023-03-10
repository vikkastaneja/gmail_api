import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
//import javax.mail

/* class to demonstrate use of Gmail list labels API */
public class GmailQuickstart {
  /**
   * Application name.
   */
  private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_SEND);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    // Load client secrets.
    InputStream in = GmailQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    //returns an authorized Credential object.
    return credential;
  }

  public static void main(String... args) throws IOException, GeneralSecurityException, MessagingException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = getCredentials(HTTP_TRANSPORT);
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();

    // Print the labels in the user's account.
    String user = "me";
    MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
    email.setFrom(new InternetAddress("vikkastaneja@gmail.com"));
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("vikkastaneja@gmail.com"));
    email.addRecipients(MimeMessage.RecipientType.TO, "vikkastaneja@gmail.com");
    email.setSubject("Test email");
    email.setText("This is a test email.");

    String rawMessage = Base64.getEncoder().encodeToString(email.toString().getBytes());

    String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
//    String url = "https://www.googleapis.com/auth/gmail.send";
    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    GenericUrl genericUrl = new GenericUrl(url);
    HttpContent content = new ByteArrayContent("application/json", String.format("{\"raw\":\"%s\"}", rawMessage).getBytes());
    HttpRequest request = requestFactory.buildPostRequest(genericUrl, content);

    request.getHeaders().setAuthorization("Bearer " + credential.getAccessToken());
//    HttpResponse response = request.execute();
//
//    if (response.isSuccessStatusCode()) {
//      System.out.println("Email sent successfully.");
//    } else {
//      System.err.println("Email not sent: " + response.getStatusMessage());
//    }


    //////////////////
    // Create the email content
    String messageSubject = "Test message";
    String bodyText = "lorem ipsum.";

    // Encode as MIME message
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage mail = new MimeMessage(session);
    mail.setFrom(new InternetAddress("vikkastaneja@gmail.com"));
    mail.addRecipient(javax.mail.Message.RecipientType.TO,
            new InternetAddress("vikkastaneja@gmail.com"));
    mail.setSubject(messageSubject);
    mail.setText(bodyText);

    // Encode and wrap the MIME message into a gmail message
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    mail.writeTo(buffer);
    byte[] rawMessageBytes = buffer.toByteArray();
    String encodedEmail = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rawMessageBytes);
    Message message = new Message();
    message.setRaw(encodedEmail);

    try {
      // Create send message
      message = service.users().messages().send("me", message).execute();
      System.out.println("Message id: " + message.getId());
      System.out.println(message.toPrettyString());
    } catch (GoogleJsonResponseException e) {
      // TODO(developer) - handle error appropriately
      GoogleJsonError error = e.getDetails();
      if (error.getCode() == 403) {
        System.err.println("Unable to send message: " + e.getDetails());
      } else {
        throw e;
      }
    }
    ///////////////////

    ListLabelsResponse listResponse = service.users().labels().list(user).execute();
    List<Label> labels = listResponse.getLabels();
    if (labels.isEmpty()) {
      System.out.println("No labels found.");
    } else {
      System.out.println("Labels:");
      for (Label label : labels) {
        System.out.printf("- %s\n", label.getName());
      }
    }
  }
}