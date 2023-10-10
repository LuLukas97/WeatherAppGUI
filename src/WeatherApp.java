import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

// get weather data from API
public class WeatherApp {
    // get weather data from given location
    public static JSONObject getWeatherData(String locationName) {
        // get location coordinates using geolocation API
        JSONArray locationData = getLocationData(locationName);

        // extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?"
        + "latitude=" + latitude + "&longitude=" + longitude
        +"&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=Europe%2FLondon";

        try {
            // call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check for response status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to the API");
                return null;
            }

            // store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                // read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            // close scanner
            scanner.close();

            // close connection
            conn.disconnect();

            // parse through data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            // get the current hours data, so we get the index of our current hour

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            // build the weather json data object for the frontend

            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;

        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray getLocationData(String locationName) {
        // replace any whitespace in given location with + sign for API request format
        locationName = locationName.replaceAll(" ", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check response status

            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            } else {
                // Store the API result
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                // read and store resulting json data into string builder
                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                // close scanner
                scanner.close();

                // close url connection
                conn.disconnect();

                // parse JSON string into JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // get list of location data the API generated from locationName
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            // connect to API
            conn.connect();
            return conn;
        } catch (IOException e){
            e.printStackTrace();
        }
        // if cant make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        // iterate through the time list and see which matches our current time

        for (int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime));
            // return the index
            return i;
        }

        return 0;
    }

    public static String getCurrentTime(){
        // get current data and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // format date to be 2023-00-00T00:00 (this is how it's read in the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        // format and print the current date and time
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }


    // convert weather code to readable text
    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if (weathercode == 0L){
            weatherCondition = "Clear";
        } else if (weathercode <= 3L && weathercode > 0L){
            weatherCondition = "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L)
        || (weathercode >= 80L && weathercode <= 99L)){
            // rain
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L){
            // snow
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
