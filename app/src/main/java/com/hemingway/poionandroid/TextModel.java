package com.hemingway.poionandroid;

import android.graphics.Color;

import java.io.Serializable;

/**
 * Description:
 * Data：2018/9/4-11:43
 * <p>
 * Author: hemingway
 */
public class TextModel implements Serializable {
    public String text;

    public boolean isBold;
    public boolean isItalic;
    public String color;
    public float fontSize;
    public String fontName;
    public boolean isUnderline;

    public int getColor(){
        if(color==null||color.length()<6){
            return 0xff000000;
        }
        int color = 0;
        try {
            color = Color.parseColor("#" + this.color);
        }catch (Exception e){
            e.printStackTrace();
        }
        return color;
    }
}
