/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.android.imageloader.display;

import me.xiaopan.android.imageloader.task.display.DisplayRequest;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

/**
 * 原图渐入位图显示器
 */
public class OriginalFadeInBitmapDisplayer implements BitmapDisplayer {
	private int duration;

	public OriginalFadeInBitmapDisplayer(int duration){
		this.duration = duration;
	}
	
	public OriginalFadeInBitmapDisplayer(){
		this(DEFAULT_ANIMATION_DURATION);
	}
	
	@Override
	public void display(ImageView imageView, BitmapDrawable bitmapDrawable, BitmapType bitmapType, DisplayRequest displayRequest) {
		Drawable oldDrawable = imageView.getDrawable();
		if(oldDrawable != null){
			TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{oldDrawable, bitmapDrawable});
        	imageView.clearAnimation();
			imageView.setImageDrawable(transitionDrawable);
			transitionDrawable.setCrossFadeEnabled(true);
			transitionDrawable.startTransition(duration);
		}else{
        	imageView.clearAnimation();
			imageView.setImageDrawable(bitmapDrawable);
		}
	}
	
	@Override
	public BitmapDisplayer copy() {
		return new OriginalFadeInBitmapDisplayer(duration);
	}
}
