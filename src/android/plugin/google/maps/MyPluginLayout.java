package plugin.google.maps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cordova.CordovaWebView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MyPluginLayout extends FrameLayout  {
  private CordovaWebView webView;
  private ViewGroup root;
  private RectF drawRect = new RectF();
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView = null;
  private FrameLayout scrollFrameLayout = null;
  private View backgroundView = null;
  private TouchableWrapper touchableWrapper;
  private ViewGroup myView = null;
  private boolean isScrolling = false;
  private ViewGroup.LayoutParams orgLayoutParams = null;
  private boolean isDebug = false;
  private Map<String, RectF> HTMLNodes = new HashMap<String, RectF>();
  
  @SuppressLint("NewApi")
  public MyPluginLayout(CordovaWebView webView) {
    super(webView.getContext());
    this.webView = webView;
    this.root = (ViewGroup) webView.getParent();
    this.context = webView.getContext();
    webView.setBackgroundColor(Color.TRANSPARENT);
    if (VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    frontLayer = new FrontLayerLayout(this.context);
    
    scrollView = new ScrollView(this.context);
    scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    backgroundView = new View(this.context);
    backgroundView.setBackgroundColor(Color.WHITE);
    backgroundView.setVerticalScrollBarEnabled(false);
    backgroundView.setHorizontalScrollBarEnabled(false);
    backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 9999));
    
    scrollFrameLayout = new FrameLayout(this.context);
    scrollFrameLayout.addView(backgroundView);
    scrollFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    
    this.touchableWrapper = new TouchableWrapper(this.context);
  }
  
  public void setDrawingRect(float left, float top, float right, float bottom) {
    this.drawRect.left = left;
    this.drawRect.top = top;
    this.drawRect.right = right;
    this.drawRect.bottom = bottom;
    this.frontLayer.invalidate();
    
  }
  
  public void putHTMLElement(String domId, float left, float top, float right, float bottom) {
    RectF rect = null;
    if (this.HTMLNodes.containsKey(domId)) {
      rect = this.HTMLNodes.get(domId);
    } else {
      rect = new RectF();
    }
    rect.left = left;
    rect.top = top;
    rect.right = right;
    rect.bottom = bottom;
    this.HTMLNodes.put(domId, rect);
  }
  public void removeHTMLElement(String domId) {
    this.HTMLNodes.remove(domId);
  }
  public void clearHTMLElement() {
    this.HTMLNodes.clear();
  }
  
  
  @SuppressWarnings("deprecation")
  public void updateViewPosition() {
    if (myView == null) {
      return;
    }
    ViewGroup.LayoutParams lParams = this.myView.getLayoutParams();
    int scrollY = webView.getScrollY();

    if (lParams instanceof AbsoluteLayout.LayoutParams) {
      AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.x = (int) this.drawRect.left;
      params.y = (int) this.drawRect.top + scrollY;
      myView.setLayoutParams(params);
    } else if (lParams instanceof LinearLayout.LayoutParams) {
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.topMargin = (int) this.drawRect.top + scrollY;
      params.leftMargin = (int) this.drawRect.left;
      myView.setLayoutParams(params);
    } else if (lParams instanceof FrameLayout.LayoutParams) {
      FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.topMargin = (int) this.drawRect.top + scrollY;
      params.leftMargin = (int) this.drawRect.left;
      params.gravity = Gravity.TOP;
      myView.setLayoutParams(params);
    } 
    if (android.os.Build.VERSION.SDK_INT < 11) {
      // Force redraw
      myView.requestLayout();
    }
    this.frontLayer.invalidate();
  }

  public View getMyView() {
    return myView;
  }
  public void setDebug(boolean debug) {
    this.isDebug = debug;
  }

  public void detachMyView() {
    if (myView == null) {
      return;
    }
    root.removeView(this);
    this.removeView(frontLayer);
    frontLayer.removeView(webView);
    
    scrollFrameLayout.removeView(myView);
    myView.removeView(this.touchableWrapper);
    
    this.removeView(this.scrollView);
    this.scrollView.removeView(scrollFrameLayout);
    if (orgLayoutParams != null) {
      myView.setLayoutParams(orgLayoutParams);
    }
    
    root.addView(webView);
    myView = null;
  }
  
  public void attachMyView(ViewGroup pluginView) {
    scrollView.scrollTo(webView.getScrollX(), webView.getScrollY());
    if (myView == pluginView) {
      return;
    } else {
      this.detachMyView();
    }
    //backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (webView.getContentHeight() * webView.getScale() + webView.getHeight())));
    
    myView = pluginView;
    ViewGroup.LayoutParams lParams = myView.getLayoutParams();
    orgLayoutParams = null;
    if (lParams != null) {
      orgLayoutParams = new ViewGroup.LayoutParams(lParams);
    }
    root.removeView(webView);
    scrollView.addView(scrollFrameLayout);
    this.addView(scrollView);
    
    pluginView.addView(this.touchableWrapper);
    scrollFrameLayout.addView(pluginView);
    
    frontLayer.addView(webView);
    this.addView(frontLayer);
    
    root.addView(this);
  }
  
  public void setPageSize(int width, int height) {
    android.view.ViewGroup.LayoutParams lParams = backgroundView.getLayoutParams();
    lParams.width = width;
    lParams.height = height;
    backgroundView.setLayoutParams(lParams);
  }
  
  public void scrollTo(int x, int y) {
    this.scrollView.scrollTo(x, y);
  }

  
  public void setBackgroundColor(int color) {
    this.backgroundView.setBackgroundColor(color);
  }
  

  private class FrontLayerLayout extends FrameLayout {
    
    public FrontLayerLayout(Context context) {
      super(context);
      this.setWillNotDraw(false);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      int x = (int)event.getX();
      int y = (int)event.getY();
      boolean contains = drawRect.contains(x, y);
      int action = event.getAction();
      isScrolling = (contains == false && action == MotionEvent.ACTION_DOWN) ? true : isScrolling;
      isScrolling = (action == MotionEvent.ACTION_UP) ? false : isScrolling;
      contains = isScrolling == true ? false : contains;
      
      if (contains) {
        // Is the touch point on any HTML elements?
        Set<Entry<String, RectF>> elements = MyPluginLayout.this.HTMLNodes.entrySet();
        Iterator<Entry<String, RectF>> iterator = elements.iterator();
        Entry <String, RectF> entry;
        while(iterator.hasNext()) {
          entry = iterator.next();
          if (entry.getValue().contains(x, y)) {
            contains = false;
            break;
          }
        }
      }
      if (!contains) {
        webView.requestFocus(View.FOCUS_DOWN);
      }
      return contains;
    }
    @Override
    protected void onDraw(Canvas canvas) {
      if (drawRect == null || isDebug == false) {
        return;
      }
      int width = canvas.getWidth();
      int height = canvas.getHeight();
      
      Paint paint = new Paint();
      paint.setColor(Color.argb(100, 0, 255, 0));
      canvas.drawRect(0f, 0f, width, drawRect.top, paint);
      canvas.drawRect(0, drawRect.top, drawRect.left, drawRect.bottom, paint);
      canvas.drawRect(drawRect.right, drawRect.top, width, drawRect.bottom, paint);
      canvas.drawRect(0, drawRect.bottom, width, height, paint);
    }
  }
  
  private class TouchableWrapper extends FrameLayout {
    
    public TouchableWrapper(Context context) {
      super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      int action = event.getAction();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
        scrollView.requestDisallowInterceptTouchEvent(true);
      }
      return super.dispatchTouchEvent(event);
    }
  } 
}