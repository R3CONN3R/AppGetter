package de.jamoo.appgetter.helpers;

import android.graphics.drawable.Drawable;

public class AppInfo
{
	String code = null;
	String name = null;
	Drawable icon;
	boolean selected = false;

	public AppInfo(String paramString1, String paramString2, Drawable paramDrawable, boolean paramBoolean)
	{
		code = paramString1;
		name = paramString2;
		icon = paramDrawable;
		selected = paramBoolean;
	}

	public String getCode()
	{
		return code;
	}

	public Drawable getImage()
	{
		return icon;
	}

	public String getName()
	{
		return name;
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void setCode(String paramString)
	{
		code = paramString;
	}

	public void setImage(Drawable paramDrawable)
	{
		icon = paramDrawable;
	}

	public void setName(String paramString)
	{
		name = paramString;
	}

	public void setSelected(boolean paramBoolean)
	{
		selected = paramBoolean;
	}


}