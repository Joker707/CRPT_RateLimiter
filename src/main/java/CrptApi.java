import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    Bucket bucket;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        Duration duration;
        switch (timeUnit) {
            case DAYS:
                duration = Duration.ofDays(1);
                break;
            case HOURS:
                duration = Duration.ofHours(1);
                break;
            case MINUTES:
                duration = Duration.ofMinutes(1);
                break;
            case SECONDS:
                duration = Duration.ofSeconds(1);
                break;
            case MILLISECONDS:
                duration = Duration.ofMillis(1);
                break;
            case MICROSECONDS:
                duration = Duration.of(1, ChronoUnit.MICROS);
                break;
            case NANOSECONDS:
                duration = Duration.ofNanos(1);
                break;
            default:
                System.out.println("Incorrect time unit");
                return;
        }
        Refill refill = Refill.intervally(requestLimit, duration);
        Bandwidth limit = Bandwidth.classic(requestLimit, refill);
        bucket = Bucket.builder().addLimit(limit).build();
    }


    public HttpResponse<String> createDocument(LP_INTRODUCE_GOODS document_content, String signature)
            throws IOException, InterruptedException {

        bucket.asBlocking().consume(1);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        String content_json = objectMapper.writeValueAsString(document_content);
        System.out.println(content_json);
        String base64 = new String(Base64.getEncoder().encode(content_json.getBytes()));
        System.out.println(base64);

        Crpt_Document crpt_document = new Crpt_Document(Document_Format.MANUAL, document_content.toString(),
                signature, "LP_INTRODUCE_GOODS");

        String document_json = objectMapper.writeValueAsString(crpt_document);
        System.out.println(document_json);

        String uri = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofString(document_json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Просто заглушка для содержимого документа, чтобы не высвечивалась ошибка в сигнатуре метода
    private class LP_INTRODUCE_GOODS {

    }


    // Объект, который отправляется по нужному адресу, при помощи HTTP клиента
    private class Crpt_Document {

        private Document_Format document_format;

        private String product_document;

        private String product_group;

        private String signature;

        private String type;

        public Crpt_Document(Document_Format document_format, String product_document, String signature, String type) {
            this.document_format = document_format;
            this.product_document = product_document;
            this.signature = signature;
            this.type = type;
        }

        public Crpt_Document(Document_Format document_format, String product_document,
                             String product_group, String signature, String type) {
            this.document_format = document_format;
            this.product_document = product_document;
            this.product_group = product_group;
            this.signature = signature;
            this.type = type;
        }

        public Crpt_Document() {
        }

        @Override
        public String toString() {
            return "Crpt_Document{" +
                    "document_format=" + document_format +
                    ", product_document='" + product_document + '\'' +
                    ", product_group='" + product_group + '\'' +
                    ", signature='" + signature + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }


    private enum Document_Format {
        MANUAL,
        XML,
        CSV
    }
}
