/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package org.apache.batik.swing;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.awt.BorderLayout;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentAdapter;
/*
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
*/

import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

import java.awt.event.ComponentEvent; 

import javax.swing.JScrollBar;
import javax.swing.Box;
import javax.swing.JPanel;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.apache.batik.bridge.ViewBox;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.JGVTComponentListener;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderListener;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;

import org.apache.batik.util.SVGConstants;

import org.w3c.dom.svg.SVGSVGElement;
import org.w3c.dom.svg.SVGDocument;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;


/**
*	This is implements a 2D scroller that will scroll an JSVGCanvas.
*	<p>
*	Reimplimentation, rather than imlementing the Scrollable interface,
*	provides several advantages. The main advantage is the ability to 
*	more precisely control ScrollBar events; fewer JSVGCanvas updates 
*	are required when scrolling. This creates a significant performance
*	(reflected by an increase in scroll speed) advantage compared to
*	implementating the Scrollable interface.
*	<p>
*	@author Zach DelProposto
*/
public class JSVGScrollPane extends JPanel
{
    protected JSVGCanvas canvas;
	
    protected JPanel horizontalPanel;
    protected JScrollBar vertical;
    protected JScrollBar horizontal;
    protected Component cornerBox;
    protected SBListener hsbListener;
    protected SBListener vsbListener;
	
    protected Rectangle2D.Float viewBox = null; // SVG Root element viewbox 
    protected boolean ignoreScrollChange = false;
	

    /**
     *	Creates a JSVGScrollPane, which will scroll an JSVGCanvas.
     *
     */
    public JSVGScrollPane(JSVGCanvas canvas)
    {
        super();
        this.canvas = canvas;
        canvas.setRecenterOnResize(false);

        // create components
        vertical = new JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, 0);
        horizontal = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 0);
		
        // create a spacer next to the horizontal bar
        horizontalPanel = new JPanel(new BorderLayout());
        horizontalPanel.add(horizontal, BorderLayout.CENTER);
        cornerBox = Box.createRigidArea
            (new Dimension(vertical.getPreferredSize().width, 
                           horizontal.getPreferredSize().height));
        horizontalPanel.add(cornerBox, BorderLayout.EAST);
		
        // listeners
        hsbListener = createScrollBarListener(false);
        horizontal.getModel().addChangeListener(hsbListener);
        horizontal.addMouseListener(hsbListener);
        horizontal.addMouseMotionListener(hsbListener);
		
        vsbListener = createScrollBarListener(true);
        vertical.getModel().addChangeListener(vsbListener);
        vertical.addMouseListener(vsbListener);
        vertical.addMouseMotionListener(vsbListener);
		
        // by default, scrollbars are not visible
        horizontalPanel.setVisible(false);
        vertical.setVisible(false);
		
        // addMouseWheelListener(new WheelListener());
		
        // layout
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(vertical, BorderLayout.EAST);
        add(horizontalPanel, BorderLayout.SOUTH);
		
        // inform of ZOOM events (to print sizes, such as in a status bar)
        canvas.addSVGDocumentLoaderListener
            (new SVGScrollDocumentLoaderListener());
		
        // canvas listeners
        ScrollListener xlistener = new ScrollListener();
        canvas.addJGVTComponentListener(xlistener);
        this.addComponentListener(xlistener);
        canvas.addGVTTreeBuilderListener(xlistener);
    }// JSVGScrollPane()


    /**
     * Scrollbar listener factory method so subclasses can
     * use a subclass of SBListener if needed.
     */
    protected SBListener createScrollBarListener(boolean isVertical) {
        return new SBListener(isVertical);
    }

    public JSVGCanvas getCanvas() {
        return canvas;
    }


    class SVGScrollDocumentLoaderListener extends SVGDocumentLoaderAdapter {
        public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
            SVGSVGElement root = e.getSVGDocument().getRootElement();
            root.addEventListener
                (SVGConstants.SVG_SVGZOOM_EVENT_TYPE, 
                 new EventListener() {
                     public void handleEvent(Event evt) {
                         if (!(evt.getTarget() instanceof SVGSVGElement))
                             return;
                         // assert(evt.getType() == 
                         //        SVGConstants.SVG_SVGZOOM_EVENT_TYPE);
                         SVGSVGElement svg = (SVGSVGElement) evt.getTarget();
                         scaleChange(svg.getCurrentScale());
                     } // handleEvent()
                 }, false);
        }// documentLoadingCompleted()			
    };
	
	
    /**
     *	Resets this object (for reloads),
     *	releasing any cached data and recomputing
     *	scroll extents.
     */
    public void reset()
    {
        viewBox = null;
        horizontalPanel.setVisible(false);
        vertical.setVisible(false);
        revalidate();
    }// reset()
	
	
    /**
     *	Sets the translation portion of the transform based upon the
     *	current scroll bar position
     */
    protected void setScrollPosition() {
        checkAndSetViewBoxRect();
        if (viewBox == null) return;

        AffineTransform crt = canvas.getRenderingTransform();
        AffineTransform vbt = canvas.getViewBoxTransform();
        if (crt == null) crt = new AffineTransform();
        if (vbt == null) vbt = new AffineTransform();

        Rectangle r2d = vbt.createTransformedShape(viewBox).getBounds();
        // System.err.println("Pre : " + r2d);
        int tx = 0, ty = 0;
        if (r2d.x < 0) tx -= r2d.x;
        if (r2d.y < 0) ty -= r2d.y;

        int deltaX = horizontal.getValue()-tx;
        int deltaY = vertical.getValue()  -ty;

        // System.err.println("tx = "+tx+"; ty = "+ty);
        // System.err.println("dx = "+deltaX+"; dy = "+deltaY);
        // System.err.println("Pre CRT: " + crt);

        crt.preConcatenate
            (AffineTransform.getTranslateInstance(-deltaX, -deltaY));
        canvas.setRenderingTransform(crt);
    }// setScrollPosition()
	
	
	
    /**
     *	MouseWheel Listener
     *	<p>
     *	Provides mouse wheel support. The mouse wheel will scroll the currently
     *	displayed scroll bar, if only one is displayed. If two scrollbars are 
     *	displayed, the mouse wheel will only scroll the vertical scrollbar.
     *
     *  This is commented out because it requires JDK 1.4 and currently
     *  Batik targets JDK 1.3.
     */
    /*
    protected class WheelListener implements MouseWheelListener
    {
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            final JScrollBar sb = (vertical.isVisible()) ? 
                vertical : horizontal;	// vertical is preferred
			
            if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                final int amt = e.getUnitsToScroll() * sb.getUnitIncrement();
                sb.setValue(sb.getValue() + amt);
            } else if(e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL){
                final int amt = e.getWheelRotation() * sb.getBlockIncrement();
                sb.setValue(sb.getValue() + amt);
            }
			
        }// mouseWheelMoved()
    }// inner class WheelListener
    */
	
	
    /**
     *	Advanced JScrollBar listener. 
     *	<p>
     *	<b>A separate listener must be attached to each scrollbar,
     *	since we keep track of mouse state for each scrollbar
     *	separately!</b> 
     *  <p> 
     *  This coalesces drag events so we don't track them, and
     *  'passes through' click events. It doesn't coalesce as many
     *  events as it should, but it helps * considerably.
     */
    protected class SBListener extends MouseAdapter 
        implements ChangeListener, MouseMotionListener
    {
        // 'true' if we are in a drag (versus a click)
        protected boolean inDrag = false; 
        // true if we are in a click
        protected boolean inClick = false;		

        protected boolean isVertical;
        int startValue;

        public SBListener(boolean vertical)
        {
            isVertical = vertical;
        }// SBListener()
			
			
        public synchronized void mouseDragged(MouseEvent e)
        {
            inDrag = true;
            AffineTransform at;
            if (isVertical) {
                int newValue = vertical.getValue();
                at = AffineTransform.getTranslateInstance
                    (0, startValue-newValue);
            } else {
                int newValue = horizontal.getValue();
                at = AffineTransform.getTranslateInstance
                    (startValue-newValue, 0);
            }

            canvas.setPaintingTransform(at);
        }// mouseDragged()
			
			
        public synchronized void mousePressed(MouseEvent e)
        {
            // we've pressed the mouse
            inClick = true;
            if (isVertical)
                startValue = vertical.getValue();
            else
                startValue = horizontal.getValue();
       }// mousePressed()
			
			
        public synchronized void mouseReleased(MouseEvent e)
        {
            if(inDrag)
                setScrollPosition(); // This is the 'end' of a drag
				
            // reset drag indicator
            inDrag = false;
            inClick = false;
        }// mouseReleased()
			
        public void mouseMoved(MouseEvent e)
        {
            // do nothing
        }// mouseMoved()
			
        public synchronized void stateChanged(ChangeEvent e)
        {
            // only respond to changes if we are NOT being dragged
            // and ignoreScrollChange is not set
            if(!inDrag && !inClick && !ignoreScrollChange) {
                //System.out.println(e);
                //System.out.println(vertical.getModel());
                //System.out.println(horizontal.getModel());
                setScrollPosition();
            }
        }// stateChanged()
    }// inner class SBListener
	
	
	
    /** Handle scroll, zoom, and resize events */
    protected class ScrollListener extends ComponentAdapter 
        implements JGVTComponentListener, GVTTreeBuilderListener
    {
        protected boolean isReady = false;
		
        public void componentTransformChanged(ComponentEvent evt)
        {
            if(isReady)
                resizeScrollBars();
        }// componentTransformChanged()
		
		
        public void componentResized(ComponentEvent evt)
        {
            if(isReady)
                resizeScrollBars();
        }// componentResized()
		
		
        public void gvtBuildCompleted(GVTTreeBuilderEvent e)
        {
            isReady = true;
            resizeScrollBars();
        }// gvtRenderingCompleted()
		
		
        public void gvtBuildCancelled(GVTTreeBuilderEvent e)
        {
            // do nothing
        }// gvtRenderingCancelled()
		
		
        public void gvtBuildFailed(GVTTreeBuilderEvent e)
        {
            // do nothing
        }// gvtRenderingFailed()
		
        public void gvtBuildPrepare(GVTTreeBuilderEvent e)
        {
            // do nothing
        }// gvtRenderingPrepare()
		
        public void gvtBuildStarted(GVTTreeBuilderEvent e)
        {
            // do nothing
        }// gvtRenderingStarted()
 		
    }// inner class ScrollListener
	
	
    /**
     *	Compute the scrollbar extents, and determine if 
     *	scrollbars should be visible.
     *
     */
    protected void resizeScrollBars()
    {
        // System.out.println("** resizeScrollBars()");

        ignoreScrollChange = true;

        checkAndSetViewBoxRect();
        if (viewBox == null) return;

        AffineTransform vbt = canvas.getViewBoxTransform();
        if (vbt == null) vbt = new AffineTransform();

        Rectangle r2d = vbt.createTransformedShape(viewBox).getBounds();
        // System.out.println("VB: " + r2d);

        // compute translation
        int maxW = r2d.width;
        int maxH = r2d.height;
        int tx = 0, ty = 0;
        if (r2d.x > 0) maxW += r2d.x;
        else           tx   -= r2d.x;
        if (r2d.y > 0) maxH += r2d.y;
        else           ty   -= r2d.y;

        // System.err.println("   maxW = "+maxW+"; maxH = "+maxH + 
        //                    " tx = "+tx+"; ty = "+ty);
        vertical.setValue(ty);
        horizontal.setValue(tx);

        // Changing scrollbar visibility may change the
        // canvas's dimensions so get the end result.
        Dimension vpSize = updateScrollbarVisibility
            (tx, ty, maxW, maxH);

        // set scroll params
        vertical.  setValues(ty, vpSize.height, 0, maxH);
        horizontal.setValues(tx, vpSize.width,  0, maxW);
		
        // set block scroll; this should be equal to a full 'page', 
        // minus a small amount to keep a portion in view
        // that small amount is 10%.
        vertical.  setBlockIncrement( (int) (0.9f * vpSize.height) );
        horizontal.setBlockIncrement( (int) (0.9f * vpSize.width) );
		
        // set unit scroll. This is arbitrary, but we define
        // it to be 20% of the current viewport. 
        vertical.  setUnitIncrement( (int) (0.2f * vpSize.height) );
        horizontal.setUnitIncrement( (int) (0.2f * vpSize.width) );
		
        ignoreScrollChange = false;
        //System.out.println("  -- end resizeScrollBars()");
    }// resizeScrollBars()

    protected Dimension updateScrollbarVisibility(int tx, int ty,
                                                  int maxW, int maxH) {
        // display scrollbars, if appropriate
        // (if scaled document size is larger than viewport size)
        // The tricky bit is ensuring that you properly track
        // the effects of making one scroll bar visible on the
        // need for the other scroll bar.

        Dimension vpSize = canvas.getSize();
        // maxVPW/H is the viewport W/H without scrollbars.
        // minVPW/H is the viewport W/H with scrollbars.
        int maxVPW = vpSize.width;  int minVPW = vpSize.width;
        int maxVPH = vpSize.height; int minVPH = vpSize.height;
        if (vertical.isVisible()) {
            maxVPW += vertical.getPreferredSize().width;
        } else {
            minVPW -= vertical.getPreferredSize().width;
        }
        if (horizontalPanel.isVisible()) {
            maxVPH += horizontal.getPreferredSize().height;
        } else {
            minVPH -= horizontal.getPreferredSize().height;
        }

        // System.err.println("W: [" + minVPW + "," + maxVPW + "] " +
        //                    "H: [" + minVPH + "," + maxVPH + "]");
        // System.err.println("MAX: [" + maxW + "," + maxH + "]");

        // Fist check if we need either scrollbar (given maxVPW/H).
        boolean vVis = (maxH > maxVPH) || (vertical.getValue() != 0);
        boolean hVis = (maxW > maxVPW) || (horizontal.getValue() != 0);
        Dimension ret = new Dimension();
        
        // This makes sure that if one scrollbar is visible
        // we 'recheck' the other scroll bar with the minVPW/H
        // since making one visible makes the room for displaying content
        // in the other dimension smaller. (This also makes the
        // 'corner box' visible if both scroll bars are visible).
        if (vVis) {
            if (hVis) {
                horizontalPanel.setVisible(true);
                vertical.setVisible(true);
                cornerBox.setVisible(true);
                ret.width  = minVPW;
                ret.height = minVPH;
            } else {
                vertical.setVisible(true);
                ret.width = minVPW;
                if (maxW > minVPW) {
                    horizontalPanel.setVisible(true);
                    cornerBox.setVisible(true);
                    ret.height = minVPH;
                } else {
                    horizontalPanel.setVisible(false);
                    cornerBox.setVisible(false);
                    ret.height = maxVPH;
                }
            }
        } else {
            if (hVis) {
                horizontalPanel.setVisible(true);
                ret.height = minVPH;
                if (maxH > minVPH) {
                    vertical.setVisible(true);
                    cornerBox.setVisible(true);
                    ret.width  = minVPW;
                } else {
                    vertical.setVisible(false);
                    cornerBox.setVisible(false);
                    ret.width  = maxVPW;
                }
            } else {
                vertical       .setVisible(false);
                horizontalPanel.setVisible(false);
                cornerBox      .setVisible(false);
                ret.width  = maxVPW;
                ret.height = maxVPH;
            }
        }

        //  Return the new size of the canvas.
        return ret;
    }
	
    /** 
     *	Derives the SVG Viewbox from the SVG root element. 
     *	Caches it. Assumes that it will not change.
     *
     */
    protected void checkAndSetViewBoxRect() {
        if (viewBox != null) return;
        SVGDocument doc = canvas.getSVGDocument();
        if (doc == null) return;
        SVGSVGElement el = doc.getRootElement();
        if (el == null) return;

        String viewBoxStr = el.getAttributeNS
            (null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE);
        float[] rect = ViewBox.parseViewBoxAttribute(el, viewBoxStr);
        viewBox = new Rectangle2D.Float(rect[0], rect[1], rect[2], rect[3]); 
        
        // System.out.println("  ** viewBox rect set: "+viewBox);
        // System.out.println("  ** doc size: "+
        //                    canvas.getSVGDocumentSize());
    }// checkAndSetViewBoxRect()
	
	
    /** 
     *	Called when the scale size changes. The scale factor
     *	(1.0 == original size). By default, this method does
     *	nothing, but may be overidden to display a scale
     *	(zoom) factor in a status bar, for example.
     */
    public void scaleChange(float scale)
    {
        // do nothing
    }
}// class JSVGScrollPane

