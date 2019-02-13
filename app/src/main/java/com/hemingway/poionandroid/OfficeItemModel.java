package com.hemingway.poionandroid;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Description:
 * Data：2018/8/27-14:28
 * <p>
 * Author: hemingway
 */
public class OfficeItemModel implements Serializable,Comparable<OfficeItemModel> {
    public static final int TYPE_OFFICE = 1;
    public static final int TYPE_OFFICE_TENCENT = 2;
    public static final int TYPE_OFFICE_WECHAT = 3;
    public static final int TYPE_OFFICE_EMPTY = 4;
    private int type = TYPE_OFFICE;

    public static final int OFFICE_TYPE_ALL = 1;
    public static final int OFFICE_TYPE_WORD = 2;
    public static final int OFFICE_TYPE_TXT = 3;
    public static final int OFFICE_TYPE_PDF = 4;
    public static final int OFFICE_TYPE_PPT = 5;

    public static final String FILE_FROM_QQ = "qqfile_recv";//QQfile_recv
    public static final String FILE_FROM_WECHAT = "micromsg/download";//MicroMsg

    private int officeType;

    public void setType(int type) {
        this.type = type;
    }


    private String name;
    private long date;
    private long size;
    private String path;
    private boolean isSelected = false;


    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getOfficeType() {
        return officeType;
    }

    public void setOfficeType(int officeType) {
        this.officeType = officeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public int compareTo(@NonNull OfficeItemModel o) {
        if(this.getDate()==o.getDate()){
            return 0;
        }else if(this.getDate()>o.getDate()){
            return -1;
        }
        return 1;
    }


}
