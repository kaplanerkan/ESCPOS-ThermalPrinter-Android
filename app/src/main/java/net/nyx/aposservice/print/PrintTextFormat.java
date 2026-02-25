package net.nyx.aposservice.print;

import android.os.Parcel;
import android.os.Parcelable;

public class PrintTextFormat implements Parcelable {
    public static final Creator<PrintTextFormat> CREATOR = new Creator<PrintTextFormat>() {
        @Override
        public PrintTextFormat createFromParcel(Parcel parcel) {
            return new PrintTextFormat(parcel);
        }

        @Override
        public PrintTextFormat[] newArray(int i) {
            return new PrintTextFormat[i];
        }
    };

    private int textSize;
    private boolean underline;
    private float textScaleX;
    private float textScaleY;
    private float letterSpacing;
    private float lineSpacing;
    private int topPadding;
    private int leftPadding;
    private int ali;
    private int style;
    private int font;
    private String path;

    @Override
    public int describeContents() {
        return 0;
    }

    public PrintTextFormat() {
        this.textSize = 24;
        this.underline = false;
        this.textScaleX = 1.0f;
        this.textScaleY = 1.0f;
        this.letterSpacing = 0.0f;
        this.lineSpacing = 1.0f;
        this.topPadding = 0;
        this.leftPadding = 0;
        this.ali = 0;
        this.style = 0;
        this.font = 0;
    }

    public int getTextSize() { return this.textSize; }
    public void setTextSize(int textSize) { this.textSize = textSize; }
    public boolean isUnderline() { return this.underline; }
    public void setUnderline(boolean underline) { this.underline = underline; }
    public float getTextScaleX() { return this.textScaleX; }
    public void setTextScaleX(float textScaleX) { this.textScaleX = textScaleX; }
    public float getTextScaleY() { return this.textScaleY; }
    public void setTextScaleY(float textScaleY) { this.textScaleY = textScaleY; }
    public float getLetterSpacing() { return this.letterSpacing; }
    public void setLetterSpacing(float letterSpacing) { this.letterSpacing = letterSpacing; }
    public float getLineSpacing() { return this.lineSpacing; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }
    public int getTopPadding() { return this.topPadding; }
    public void setTopPadding(int topPadding) { this.topPadding = topPadding; }
    public int getLeftPadding() { return this.leftPadding; }
    public void setLeftPadding(int leftPadding) { this.leftPadding = leftPadding; }
    public int getAli() { return this.ali; }
    public void setAli(int ali) { this.ali = ali; }
    public int getStyle() { return this.style; }
    public void setStyle(int style) { this.style = style; }
    public int getFont() { return this.font; }
    public void setFont(int font) { this.font = font; }
    public String getPath() { return this.path; }
    public void setPath(String path) { this.path = path; }

    protected PrintTextFormat(Parcel parcel) {
        this();
        this.textSize = parcel.readInt();
        this.underline = parcel.readByte() != 0;
        this.textScaleX = parcel.readFloat();
        this.textScaleY = parcel.readFloat();
        this.letterSpacing = parcel.readFloat();
        this.lineSpacing = parcel.readFloat();
        this.topPadding = parcel.readInt();
        this.leftPadding = parcel.readInt();
        this.ali = parcel.readInt();
        this.style = parcel.readInt();
        this.font = parcel.readInt();
        this.path = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.textSize);
        parcel.writeByte(this.underline ? (byte) 1 : (byte) 0);
        parcel.writeFloat(this.textScaleX);
        parcel.writeFloat(this.textScaleY);
        parcel.writeFloat(this.letterSpacing);
        parcel.writeFloat(this.lineSpacing);
        parcel.writeInt(this.topPadding);
        parcel.writeInt(this.leftPadding);
        parcel.writeInt(this.ali);
        parcel.writeInt(this.style);
        parcel.writeInt(this.font);
        parcel.writeString(this.path);
    }
}
