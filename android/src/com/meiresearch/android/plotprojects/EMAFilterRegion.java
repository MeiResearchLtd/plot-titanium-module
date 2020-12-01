package com.meiresearch.android.plotprojects;

public class EMAFilterRegion {

    private static String custom_healthkick = "custom_healthkick";

    // return true if region is allowed.
    // return false if region isn't allowed.
    // this is designed for a corner case with healthkick to filter out some combination region names.
    public static boolean regionAllowed(String region_name){
        String pp_val = EMADataAccess.getStringProperty("plotProjects.project");

        if(pp_val.equals(custom_healthkick)){
            return healthkickFilter(region_name);
        }

        return true;
    }

    // this is a custom function only for STTR healthkick but we don't have an easy mechanism
    // to include 'custom' code in these native modules.
    // this whitelists only certain regions and disallows all others.
    private static boolean healthkickFilter(String region_name){

        String name = region_name.replace("[", "").replace("]", "");

        switch(name.toLowerCase()){
            case "tacobell":
            case "wendys":
            case "subway":
            case "mcdonalds":
            case "burgerking":
            case "kfc":
                return true;
            default:
                return false;
        }
    }
}
