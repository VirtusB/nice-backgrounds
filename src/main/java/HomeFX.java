import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;


public class HomeFX {
    public CheckBox activeCheckbox;
    public ComboBox searchTermCb;
    public Button nextImageButton;
    public ComboBox updateIntervalCb;

    public Preferences preferences;

    private ScheduledExecutorService updateIntervalExecutor = Executors.newSingleThreadScheduledExecutor();

    private ObservableList<UpdateInterval> updateIntervalOptions =
            FXCollections.observableArrayList(
                    new UpdateInterval("Stopped", 0, 1),
                    new UpdateInterval("Every 30 sec.", 30, 2),
                    new UpdateInterval("Every 2 min.", 120, 3),
                    new UpdateInterval("Every 30 min.", 1800, 4),
                    new UpdateInterval("Every 2 hrs.", 7200, 5)
            );

    private ObservableList<String> searchOptions =
            FXCollections.observableArrayList(
                    "Random",
                    "Cars",
                    "Nature",
                    "Art",
                    "Code",
                    "Spectrums",
                    "Abstract",
                    "Night",
                    "America"
            );

    @FXML
    protected void initialize() {
        addComboBoxOptions();

        setupNextImageButton();

        addActiveCheckBoxListener();
        addSearchTermComboBoxListener();
        addNextButtonListener();
        addUpdateIntervalListener();

        setupPreferences();
    }

    private void setupPreferences() {
        this.preferences = Preferences.userNodeForPackage(HomeFX.class);

        boolean activeStatus = this.preferences.getBoolean("isActive", false);
        this.setActiveStatusManually(activeStatus);

        String searchTerm = this.preferences.get("searchTerm", "Random");
        this.setSearchTermByValue(searchTerm);

        int updateIntervalId = this.preferences.getInt("updateIntervalId", -1);
        if (updateIntervalId != -1) {
            UpdateInterval interval = findUpdateIntervalById(this.updateIntervalOptions, updateIntervalId);
            this.updateIntervalCb.getSelectionModel().select(interval);
        }
    }

    public UpdateInterval findUpdateIntervalById(Collection<UpdateInterval> listInterval, int id) {
        return listInterval.stream().filter(interval -> id == interval.get_id()).findFirst().orElse(null);
    }


    private void addComboBoxOptions() {
        searchTermCb.getItems().addAll(searchOptions);
        updateIntervalCb.getItems().addAll(updateIntervalOptions);
    }

    private boolean isActive() {
        return this.activeCheckbox.isSelected();
    }

    private void setActiveStatusManually(boolean active) {
        this.activeCheckbox.setSelected(active);
    }

    private void addUpdateInterval() {
        if (this.isActive()) {

            if (!updateIntervalExecutor.isShutdown()) {
                updateIntervalExecutor.shutdownNow();
            }

            updateIntervalExecutor = Executors.newSingleThreadScheduledExecutor();

            UpdateInterval updateInterval = (UpdateInterval) this.updateIntervalCb.getValue();
            if (updateInterval == null || updateInterval.get_intervalSeconds() == 0) {
                return;
            }


            int ms = updateInterval.get_intervalSeconds() * 1000;

            Runnable task = () -> {
                getImageAndSetBackground();
            };

            updateIntervalExecutor.scheduleAtFixedRate(task, ms, ms, TimeUnit.MILLISECONDS);
        } else if (updateIntervalExecutor != null && !updateIntervalExecutor.isShutdown()) {
            updateIntervalExecutor.shutdownNow();
        }
    }

    private void addUpdateIntervalListener() {
        this.updateIntervalCb.setOnAction(event -> {
            addUpdateInterval();
            System.out.println(1);

            UpdateInterval selectedUpdateInterval = (UpdateInterval) this.updateIntervalCb.getSelectionModel().getSelectedItem();
            this.preferences.putInt("updateIntervalId", selectedUpdateInterval.get_id());
        });
    }

    private void setupNextImageButton() {
        ImageView iconNext = new ImageView("/next-icon.png");
        nextImageButton.setGraphic(iconNext);
        nextImageButton.setContentDisplay(ContentDisplay.RIGHT);

        iconNext.getStyleClass().add("graphic");
        iconNext.setFitWidth(22);
        iconNext.setFitHeight(22);
    }

    private void addNextButtonListener() {
        this.nextImageButton.setOnAction(event -> {
            if (this.isActive()) {
                getImageAndSetBackground();
            }
        });
    }

    private void addSearchTermComboBoxListener() {
        this.searchTermCb.setOnAction(event -> {
            if (this.isActive()) {
                getImageAndSetBackground();
            }
        });
    }

    private void setSearchTermByValue(String searchTerm) {
        this.searchTermCb.getSelectionModel().select(searchTerm);
    }

    private void getImageAndSetBackground() {
        if (this.isActive()) {
            String cacheDir = System.getProperty("user.dir") + "\\src\\main\\resources\\cache";

            String searchTerm = getSelectedSearchTerm();
            int page = getRandomNumber(getTotalCountOfPhotos(searchTerm));

            String photoUrl = getPhotoUrl(page, searchTerm);


            BufferedImage image = downloadPhoto(photoUrl);
            String photoPath = savePhoto(image, cacheDir);
            setBackgroundImage(photoPath);
        }
    }

    static private double nextSkewedBoundedDouble(double min, double max, double skew, double bias) {
        Random r = new Random();
        double range = max - min;
        double mid = min + range / 2.0;
        double unitGaussian = r.nextGaussian();
        double biasFactor = Math.exp(bias);
        double retval = mid+(range*(biasFactor/(biasFactor+Math.exp(-unitGaussian/skew))-0.5));
        return retval;
    }

    private String getSelectedSearchTerm() {
        if (searchTermCb.getSelectionModel().getSelectedItem() != null && !searchTermCb.getSelectionModel().getSelectedItem().toString().equals("Random")) {
            String searchTerm = searchTermCb.getSelectionModel().getSelectedItem().toString();
            this.preferences.put("searchTerm", searchTerm);

            return searchTerm;
        }
        return searchOptions.get(new Random().nextInt(searchOptions.size()));
    }


    private void addActiveCheckBoxListener() {
        this.activeCheckbox.selectedProperty().addListener((oldVal, old, newv) -> {
            getImageAndSetBackground();
            addUpdateInterval();
            this.preferences.putBoolean("isActive", this.isActive());
        });
    }

    private int getRandomNumber(int max) {
        double rf = nextSkewedBoundedDouble(0, max, 1.0, -4.0);
        return (int) rf;
    }

    private int getTotalCountOfPhotos(String search_term) {
        String accessKey = "dcf5392879d1ba569f2c753332bb1b3a5fd488f840d8629e5815438d79fc6a36";
        URL url;
        StringBuffer response = new StringBuffer();

        try {
            url = new URL(String.format("https://api.unsplash.com/search/photos?page=1&query=%s&per_page=1&orientation=landscape&client_id=%s", search_term, accessKey));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String temp;

            while ((temp = in.readLine()) != null) {
                response.append(temp);
            }

            in.close();

            con.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JSONObject(response.toString()).getInt("total");
    }



    private BufferedImage downloadPhoto(String photoUrl) {
        BufferedImage image = null;

        try {
            URL url = new URL(photoUrl);
            image = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    private String savePhoto(BufferedImage image, String path) {
        String fullPath = path + "\\image.bmp";
        File outputfile = new File(fullPath);

        try {
            ImageIO.write(image, "bmp", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullPath;
    }

    private String getPhotoUrl(int page, String search_term) {
        String accessKey = "dcf5392879d1ba569f2c753332bb1b3a5fd488f840d8629e5815438d79fc6a36";
        URL url;
        StringBuffer response = new StringBuffer();

        try {
            url = new URL(String.format("https://api.unsplash.com/search/photos?page=%d&query=%s&per_page=1&orientation=landscape&client_id=%s", page, search_term, accessKey));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String temp;

            while ((temp = in.readLine()) != null) {
                response.append(temp);
            }

            in.close();

            con.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JSONObject(response.toString()).getJSONArray("results").getJSONObject(0).getJSONObject("urls").getString("full");
    }

    private void setBackgroundImage(String path) {
        WallpaperChanger.Change(path);
    }

}
