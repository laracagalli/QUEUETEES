package model;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Estimated straight-line distance between Pasay and a destination city/municipality. */
public final class WarehouseDistance {
    private static final double LAT=14.5333333333,LON=121.0;
    private static final Map<String,double[]> COORDINATES=load();
    private WarehouseDistance(){}
    private static Map<String,double[]> load(){Map<String,double[]> result=new HashMap<>();
        try(var in=WarehouseDistance.class.getResourceAsStream("/data/delivery-coordinates.tsv")){
            if(in==null)throw new IllegalStateException("Delivery coordinate directory missing.");
            try(var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){reader.readLine();String line;
                while((line=reader.readLine())!=null){String[] p=line.split("\t");result.put(p[0]+"|"+p[1],new double[]{Double.parseDouble(p[2]),Double.parseDouble(p[3])});}
            }
        }catch(IOException ex){throw new IllegalStateException("Cannot read delivery coordinates.",ex);}return Map.copyOf(result);
    }
    public static double kilometres(Order order){DeliveryAddress a=order.getCheckoutDetails().getStructuredAddress();
        if(a==null)return order.getCheckoutDetails().isPickup()?0:Double.POSITIVE_INFINITY;
        double[] destination=COORDINATES.get(a.getProvince()+"|"+a.getCity());if(destination==null)return Double.POSITIVE_INFINITY;
        double lat=Math.toRadians(destination[0]-LAT),lon=Math.toRadians(destination[1]-LON);
        double h=Math.pow(Math.sin(lat/2),2)+Math.cos(Math.toRadians(LAT))*Math.cos(Math.toRadians(destination[0]))*Math.pow(Math.sin(lon/2),2);
        return 6371*2*Math.asin(Math.sqrt(Math.min(1,h)));
    }
    public static String label(Order order){double km=kilometres(order);return Double.isFinite(km)?String.format(Locale.ENGLISH,"Est. %.1f km from Pasay",km):"Distance requires review";}
}
