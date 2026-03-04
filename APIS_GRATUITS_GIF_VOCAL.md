# 🎁 APIs Gratuites pour GIFs et Messages Vocaux

**Date** : 04 Mars 2026  
**Application** : LAMMA Voyage Messagerie

---

## 🎁 APIs GRATUITES pour GIFs

### 1️⃣ **Tenor API** (⭐ RECOMMANDÉE)

**Meilleure option pour votre app**

#### Avantages
- ✅ 100% gratuit
- ✅ Pas de limite strict (API généreux)
- ✅ Qualité GIFs excellente
- ✅ Fast CDN global
- ✅ Facile à intégrer

#### Inscription
```
1. Aller sur : https://tenor.com/developer/dashboard
2. Créer compte Google/Email
3. Créer une application
4. Obtenir API Key (gratuit)
```

#### Utilisation Java
```java
// URL de recherche
String apiKey = "VOTRE_TENOR_API_KEY";
String query = "happy";  // Terme recherché
String url = "https://api.tenor.com/v1/search?q=" + 
    URLEncoder.encode(query, "UTF-8") + 
    "&key=" + apiKey + 
    "&limit=20";

// Faire requête HTTP
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());

// Parser JSON (voir exemple plus bas)
```

#### Réponse JSON (exemple)
```json
{
  "results": [
    {
      "id": "14827",
      "title": "Happy dancing",
      "media": [
        {
          "gif": {
            "url": "https://media.tenor.com/..."
          }
        }
      ]
    }
  ]
}
```

#### Intégration dans votre app
```java
// Dans GifPickerController.java
private void searchGifs(String query) {
    String apiKey = "YOUR_API_KEY";
    String url = "https://api.tenor.com/v1/search?q=" + 
        URLEncoder.encode(query, "UTF-8") + 
        "&key=" + apiKey + 
        "&limit=20";
    
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();
    
    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenAccept(response -> {
            // Parser et afficher GIFs
            displayGifs(response.body());
        });
}
```

---

### 2️⃣ **Giphy API** (Alternative)

**Bonne option si vous préférez Giphy**

#### Avantages
- ✅ API libre (free tier)
- ✅ Meilleure UI web
- ✅ Trending endpoint

#### Inscription
```
1. Aller sur : https://developers.giphy.com
2. Créer compte
3. Créer application
4. Obtenir API Key
```

#### URL Base
```
https://api.giphy.com/v1/gifs/search?api_key=YOUR_KEY&q=search_term&limit=20
```

#### Réponse
```json
{
  "data": [
    {
      "id": "l0HlMZy2ZtbzM",
      "url": "https://giphy.com/gifs/...",
      "images": {
        "original": {
          "url": "https://media.giphy.com/..."
        }
      }
    }
  ]
}
```

---

### Comparaison Tenor vs Giphy

| Critère | Tenor | Giphy |
|---------|-------|-------|
| **Gratuit** | ✅ Oui | ✅ Oui |
| **Limite requêtes** | Généreux | 43 /jour libre |
| **Qualité GIFs** | Excellente | Très bonne |
| **Facilité intégration** | Très facile | Facile |
| **Documentation** | Excellente | Bonne |
| **Vitesse API** | Très rapide | Rapide |
| **Recommandation** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

**👉 UTILISEZ TENOR (meilleur choix gratuit)**

---

## 🎤 APIs GRATUITES pour Messages Vocaux

### Option 1️⃣ : Enregistrement Local (RECOMMANDÉ)

**Avantage** : Pas d'API externe, tout local

#### Bibliothèque : javax.sound.sampled (Built-in Java)

```java
import javax.sound.sampled.*;
import java.io.File;

public class VoiceRecorder {
    
    private AudioFormat format;
    private TargetDataLine targetDataLine;
    private boolean recording = false;
    
    public VoiceRecorder() {
        // Initialiser le format audio (16-bit PCM, 44.1kHz, mono)
        format = new AudioFormat(44100, 16, 1, true, false);
    }
    
    public void startRecording(String outputFile) throws Exception {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(format);
        targetDataLine.start();
        
        recording = true;
        
        // Thread d'enregistrement
        new Thread(() -> {
            try {
                AudioInputStream audioInputStream = 
                    new AudioInputStream(targetDataLine);
                AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
                AudioSystem.write(audioInputStream, fileType, new File(outputFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public void stopRecording() {
        recording = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
    }
}
```

#### Avantages
- ✅ Zéro dépendance externe
- ✅ Fichiers stockés localement
- ✅ Intimité données
- ✅ Pas de limite API
- ✅ Format WAV standard

---

### Option 2️⃣ : Web Speech API (Browser-based)

**Si vous avez une version web**

```html
<script>
const recognition = new webkitSpeechRecognition();
recognition.onresult = function(event) {
    let transcript = '';
    for (let i = event.resultIndex; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
    }
    console.log(transcript);
};
recognition.start();
</script>
```

---

### Option 3️⃣ : Google Cloud Speech-to-Text (API Gratuite)

**Pour transcrire en texte**

#### Inscription
```
1. Aller sur : https://cloud.google.com/speech-to-text
2. Créer compte Google
3. Activer API Speech-to-Text
4. Créer credentials (Service Account JSON)
```

#### Utilisation Java
```java
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TranscribeAudio {
    public static void main(String... args) throws Exception {
        try (SpeechClient speechClient = SpeechClient.create()) {
            
            // Charger fichier audio
            String fileName = "audio.wav";
            byte[] audioBytes = Files.readAllBytes(Paths.get(fileName));
            ByteString audioData = ByteString.copyFrom(audioBytes);
            
            // Configuration
            RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(44100)
                .setLanguageCode("fr-FR")
                .build();
            
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioData)
                .build();
            
            // Transcrire
            RecognizeResponse response = speechClient.recognize(config, audio);
            
            // Afficher résultat
            for (SpeechRecognitionResult result : response.getResultsList()) {
                for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
                    System.out.println("Transcript: " + alternative.getTranscript());
                }
            }
        }
    }
}
```

#### Dépendance Maven
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-speech</artifactId>
    <version>2.3.0</version>
</dependency>
```

---

### Comparaison Options Vocales

| Option | Coût | Setup | Performance | Recommandation |
|--------|------|-------|-------------|---|
| **Enregistrement local** | Gratuit | Simple | Excellent | ⭐⭐⭐⭐⭐ |
| **Web Speech API** | Gratuit | Facile | Bon | ⭐⭐⭐⭐ |
| **Google Cloud** | Gratuit (300$) | Complexe | Très bon | ⭐⭐⭐ |

**👉 UTILISEZ Enregistrement Local (javax.sound.sampled)**

---

## 📋 Plan d'Implémentation

### Phase 2.1 : GIFs (Tenor)

```java
// 1. Ajouter dépendance pour JSON parsing
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20230227</version>
</dependency>

// 2. Créer GifService.java
class GifService {
    private static final String API_KEY = "your_tenor_key";
    
    public List<GifResult> searchGifs(String query) {
        // Appel API Tenor
        // Parser résultat
        // Retourner liste GIFs
    }
}

// 3. Afficher dans GifPickerController.java
// Grille d'images cliquables
// Clic → envoyer GIF dans chat
```

### Phase 2.2 : Messages Vocaux

```java
// 1. Créer VoiceRecorder.java avec javax.sound

// 2. Ajouter UI ChatController.java
// Bouton 🎤 "Enregistrer"
// Bouton ⏹️ "Arrêter"
// Affichage waveform simple

// 3. Stocker en BD
// type_message = "AUDIO"
// fichier_path = path_to_wav
// Affichage : bouton play ▶️

// 4. Playback
// Clic sur message audio
// Joue avec javax.sound.sampled
```

---

## 🔑 Clés API Recommandées

### Tenor API Key
```
Gratuit, illimité, pas d'expiration
Obtenir : https://tenor.com/developer/dashboard
```

### Configuration dans le code
```java
// Dans GifPickerController.java
public class GifPickerController {
    private static final String TENOR_API_KEY = "YOUR_KEY_HERE";
    // Ou : charger depuis fichier config
}
```

---

## 📚 Ressources Utiles

### Tenor API
- Documentation : https://tenor.com/developer/documentation
- Endpoint Search : `/v1/search`
- Endpoint Trending : `/v1/trending`

### Java Audio
- Documentation : https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/package-summary.html
- Tutorial : https://docs.oracle.com/javase/tutorial/sound/

### Google Cloud
- Documentation : https://cloud.google.com/speech-to-text/docs
- Exemples : https://github.com/GoogleCloudPlatform/java-docs-samples/tree/main/speech

---

## 🚀 Prochaines Étapes

1. **GIFs** :
   - [ ] S'inscrire sur Tenor
   - [ ] Obtenir API Key
   - [ ] Implémenter GifService.java
   - [ ] Connecter à GifPickerController.java
   - [ ] Tester recherche + envoi

2. **Messages Vocaux** :
   - [ ] Créer VoiceRecorder.java
   - [ ] UI bouton 🎤 ⏹️
   - [ ] Enregistrement WAV
   - [ ] Stockage BD
   - [ ] Playback intégré

---

## ✅ Récapitulatif

| Fonctionnalité | API Recommandée | État | Coût |
|---|---|---|---|
| **GIFs** | Tenor | À implémenter | Gratuit |
| **Messages Vocaux** | javax.sound (local) | À implémenter | Gratuit |
| **Transcrire Vocal** | Google Cloud (optionnel) | Optional | 300$/mois free |

**Démarrez avec Tenor (GIF) + javax.sound (Audio) 🚀**

---

**Champion, vous avez maintenant:**
- ✅ Couleurs visibles pour icônes, titres, descriptions
- ✅ APIs gratuites recommandées pour GIFs et vocal
- ✅ Code exemple prêt à copier/coller

Bon développement ! 🎊

