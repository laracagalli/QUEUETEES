package model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline address directory. Zones are operational coverage, not administrative regions. */
public final class DeliveryAddress {
    private static final Map<String, Map<String, Map<String, DeliveryPoint>>> DIRECTORY = load();
    private final String province, city, barangay, street;
    public DeliveryAddress(String province, String city, String barangay, String street) {
        this.province = clean(province); this.city = clean(city);
        this.barangay = clean(barangay); this.street = clean(street);
        if (getDeliveryPoint() == DeliveryPoint.UNASSIGNED)
            throw new IllegalArgumentException("Select a covered province, city/municipality and barangay.");
        if (this.street.isEmpty() || this.street.length() > 100)
            throw new IllegalArgumentException("House number / street is required (maximum 100 characters).");
    }
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static Map<String, Map<String, Map<String, DeliveryPoint>>> load() {
        Map<String, Map<String, Map<String, DeliveryPoint>>> data = new TreeMap<>();
        try (InputStream input = DeliveryAddress.class.getResourceAsStream("/data/delivery-addresses.tsv")) {
            if (input == null) throw new IllegalStateException("Address directory missing. Include src on the runtime classpath.");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.readLine(); String line;
                while ((line = reader.readLine()) != null) {
                    String[] p = line.split("\t", -1);
                    if (p.length != 4) throw new IOException("Invalid address directory entry");
                    data.computeIfAbsent(p[0], k -> new TreeMap<>()).computeIfAbsent(p[1], k -> new TreeMap<>())
                        .put(p[2], DeliveryPoint.valueOf(p[3]));
                }
            }
        } catch (IOException ex) { throw new IllegalStateException("Cannot read address directory", ex); }
        return data;
    }
    public static String[] provinces() { return DIRECTORY.keySet().toArray(new String[0]); }
    /** Read the same format saved by registration, preserving commas in the street. */
    public static Optional<DeliveryAddress> fromSavedAddress(String value) {
        if(value==null || value.isBlank())return Optional.empty();
        String remaining=value.trim();
        for(String province:provinces()) {
            String provinceSuffix=", "+province;
            if(!remaining.endsWith(provinceSuffix))continue;
            String beforeProvince=remaining.substring(0,remaining.length()-provinceSuffix.length());
            for(String city:cities(province)) {
                String citySuffix=", "+city;
                if(!beforeProvince.endsWith(citySuffix))continue;
                String beforeCity=beforeProvince.substring(0,beforeProvince.length()-citySuffix.length());
                String[] barangays=barangays(province,city);
                Arrays.sort(barangays,Comparator.comparingInt(String::length).reversed());
                for(String barangay:barangays) {
                    String barangaySuffix=", "+barangay;
                    if(!beforeCity.endsWith(barangaySuffix))continue;
                    try{return Optional.of(new DeliveryAddress(province,city,barangay,beforeCity.substring(0,beforeCity.length()-barangaySuffix.length())));}
                    catch(IllegalArgumentException invalid){return Optional.empty();}
                }
            }
        }
        return Optional.empty();
    }
    public static String[] cities(String province) { return DIRECTORY.getOrDefault(province, Collections.emptyMap()).keySet().toArray(new String[0]); }
    public static String[] barangays(String province, String city) { return DIRECTORY.getOrDefault(province, Collections.emptyMap()).getOrDefault(city, Collections.emptyMap()).keySet().toArray(new String[0]); }
    public static DeliveryPoint pointForProvince(String province) {
        return DIRECTORY.getOrDefault(province, Collections.emptyMap()).values().stream().flatMap(m -> m.values().stream()).findFirst().orElse(DeliveryPoint.UNASSIGNED);
    }
    public DeliveryPoint getDeliveryPoint() { return DIRECTORY.getOrDefault(province, Collections.emptyMap()).getOrDefault(city, Collections.emptyMap()).getOrDefault(barangay, DeliveryPoint.UNASSIGNED); }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getBarangay() { return barangay; }
    public String getStreet() { return street; }
    public String format() { return street + ", " + barangay + ", " + city + ", " + province; }
}
