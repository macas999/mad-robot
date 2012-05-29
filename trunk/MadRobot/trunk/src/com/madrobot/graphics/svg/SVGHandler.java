package com.madrobot.graphics.svg;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.Log;

import com.madrobot.di.XMLUtils;
import com.madrobot.graphics.ColorUtils;
import com.madrobot.graphics.GraphicsUtils;

class SVGHandler extends DefaultHandler {
	private static final String TAG = "MadRobot";
	HashMap<String, String> idXml = new HashMap<String, String>();

	private Picture picture;
	private Canvas canvas;

	private Paint strokePaint;
	private boolean strokeSet = false;
	private Stack<Paint> strokePaintStack = new Stack<Paint>();
	private Stack<Boolean> strokeSetStack = new Stack<Boolean>();

	private Paint fillPaint;
	private boolean fillSet = false;
	private Stack<Paint> fillPaintStack = new Stack<Paint>();
	private Stack<Boolean> fillSetStack = new Stack<Boolean>();

	// Scratch rect (so we aren't constantly making new ones)
	private RectF rect = new RectF();
	RectF bounds = null;
	RectF limits = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
			Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

	private Integer searchColor = null;
	private Integer replaceColor = null;

	private boolean whiteMode = false;

	private int pushed = 0;

	private boolean hidden = false;
	private int hiddenLevel = 0;
	private boolean boundsMode = false;

	private HashMap<String, Shader> gradientMap = new HashMap<String, Shader>();
	private HashMap<String, SVGGradient> gradientRefMap = new HashMap<String, SVGGradient>();
	private SVGGradient gradient = null;
	private SVGText text = null;

	private boolean inDefsElement = false;
	private int zoomFactor;

	SVGHandler(Picture picture, int zoomFactor) {
		this.picture = picture;
		strokePaint = new Paint();
		strokePaint.setAntiAlias(true);
		strokePaint.setStyle(Paint.Style.STROKE);
		fillPaint = new Paint();
		fillPaint.setAntiAlias(true);
		fillPaint.setStyle(Paint.Style.FILL);
		this.zoomFactor = zoomFactor;
	}

	private Float getZoomFactor(Float value) {
		if (value == null)
			return null;
		value = value * zoomFactor;
		return value / 100;
	}

	/**
	 * Parses a single SVG path and returns it as a
	 * <code>android.graphics.Path</code> object. An example path is
	 * <code>M250,150L150,350L350,350Z</code>, which draws a triangle.
	 * 
	 * @param pathString
	 *            the SVG path, see the specification <a
	 *            href="http://www.w3.org/TR/SVG/paths.html">here</a>.
	 */
	private Path parsePath(String pathString) {
		return doPath(pathString);
	}

	/**
	 * This is where the hard-to-parse paths are handled. Uppercase rules are
	 * absolute positions, lowercase are relative. Types of path rules:
	 * <p/>
	 * <ol>
	 * <li>M/m - (x y)+ - Move to (without drawing)
	 * <li>Z/z - (no params) - Close path (back to starting point)
	 * <li>L/l - (x y)+ - Line to
	 * <li>H/h - x+ - Horizontal ine to
	 * <li>V/v - y+ - Vertical line to
	 * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
	 * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes
	 * the x2, y2 from previous C/S is the x1, y1 of this bezier)
	 * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
	 * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control
	 * point is "reflection" of last one w.r.t. to current point)
	 * </ol>
	 * <p/>
	 * Numbers are separate by whitespace, comma or nothing at all (!) if they
	 * are self-delimiting, (ie. begin with a - sign)
	 * 
	 * @param s
	 *            the path string from the XML
	 */
	private Path doPath(String s) {
		int n = s.length();
		ParserHelper ph = new ParserHelper(s, 0);
		ph.skipWhitespace();
		Path p = new Path();
		float lastX = 0;
		float lastY = 0;
		float lastX1 = 0;
		float lastY1 = 0;
		RectF r = new RectF();
		char cmd = 'x';
		while (ph.pos < n) {
			char next = s.charAt(ph.pos);
			if (!Character.isDigit(next) && !(next == '.') && !(next == '-')) {
				cmd = next;
				ph.advance();
			} else if (cmd == 'M') { // implied command
				cmd = 'L';
			} else if (cmd == 'm') { // implied command
				cmd = 'l';
			} else { // implied command
						// Log.d(TAG, "Implied command: " + cmd);
			}
			p.computeBounds(r, true);
			// Log.d(TAG, "  " + cmd + " " + r);
			// Util.debug("* Commands remaining: '" + path + "'.");
			boolean wasCurve = false;
			switch (cmd) {
			case 'M':
			case 'm': {
				float x = getZoomFactor(ph.nextFloat());
				float y = getZoomFactor(ph.nextFloat());
				if (cmd == 'm') {
					p.rMoveTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.moveTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'Z':
			case 'z': {
				p.close();
				break;
			}
			case 'L':
			case 'l': {
				float x =getZoomFactor( ph.nextFloat());
				float y = getZoomFactor(ph.nextFloat());
				if (cmd == 'l') {
					p.rLineTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.lineTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'H':
			case 'h': {
				float x = getZoomFactor(ph.nextFloat());
				if (cmd == 'h') {
					p.rLineTo(x, 0);
					lastX += x;
				} else {
					p.lineTo(x, lastY);
					lastX = x;
				}
				break;
			}
			case 'V':
			case 'v': {
				float y = getZoomFactor(ph.nextFloat());
				if (cmd == 'v') {
					p.rLineTo(0, y);
					lastY += y;
				} else {
					p.lineTo(lastX, y);
					lastY = y;
				}
				break;
			}
			case 'C':
			case 'c': {
				wasCurve = true;
				float x1 = getZoomFactor(ph.nextFloat());
				float y1 = getZoomFactor(ph.nextFloat());
				float x2 = getZoomFactor(ph.nextFloat());
				float y2 = getZoomFactor(ph.nextFloat());
				float x = getZoomFactor(ph.nextFloat());
				float y = getZoomFactor(ph.nextFloat());
				if (cmd == 'c') {
					x1 += lastX;
					x2 += lastX;
					x += lastX;
					y1 += lastY;
					y2 += lastY;
					y += lastY;
				}
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'S':
			case 's': {
				wasCurve = true;
				float x2 = getZoomFactor(ph.nextFloat());
				float y2 = getZoomFactor(ph.nextFloat());
				float x = getZoomFactor(ph.nextFloat());
				float y = getZoomFactor(ph.nextFloat());
				if (cmd == 's') {
					x2 += lastX;
					x += lastX;
					y2 += lastY;
					y += lastY;
				}
				float x1 = 2 * lastX - lastX1;
				float y1 = 2 * lastY - lastY1;
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'A':
			case 'a': {
				float rx = ph.nextFloat();
				float ry = ph.nextFloat();
				float theta = ph.nextFloat();
				int largeArc = (int) ph.nextFloat();
				int sweepArc = (int) ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'a') {
					x += lastX;
					y += lastY;
				}
				GraphicsUtils.drawArc(p, lastX, lastY, x, y, rx, ry, theta,
						largeArc == 1, sweepArc == 1);
				lastX = x;
				lastY = y;
				break;
			}
			default:
				Log.d(TAG, "Invalid path command: " + cmd);
				ph.advance();
			}
			if (!wasCurve) {
				lastX1 = lastX;
				lastY1 = lastY;
			}
			ph.skipWhitespace();
		}
		return p;
	}

	private NumberParser getNumberParseAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return parseNumbers(attributes.getValue(i));
			}
		}
		return null;
	}

	private NumberParser parseNumbers(String s) {
		// Util.debug("Parsing numbers from: '" + s + "'");
		int n = s.length();
		int p = 0;
		ArrayList<Float> numbers = new ArrayList<Float>();
		boolean skipChar = false;
		for (int i = 1; i < n; i++) {
			if (skipChar) {
				skipChar = false;
				continue;
			}
			char c = s.charAt(i);
			switch (c) {
			// This ends the parsing, as we are on the next element
			case 'M':
			case 'm':
			case 'Z':
			case 'z':
			case 'L':
			case 'l':
			case 'H':
			case 'h':
			case 'V':
			case 'v':
			case 'C':
			case 'c':
			case 'S':
			case 's':
			case 'Q':
			case 'q':
			case 'T':
			case 't':
			case 'a':
			case 'A':
			case ')': {
				String str = s.substring(p, i);
				if (str.trim().length() > 0) {
					// Util.debug("  Last: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
				}
				p = i;
				return new NumberParser(numbers, p);
			}
			case '\n':
			case '\t':
			case ' ':
			case ',': {
				String str = s.substring(p, i);
				// Just keep moving if multiple whitespace
				if (str.trim().length() > 0) {
					// Util.debug("  Next: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
					if (c == '-') {
						p = i;
					} else {
						p = i + 1;
						skipChar = true;
					}
				} else {
					p++;
				}
				break;
			}
			}
		}
		String last = s.substring(p);
		if (last.length() > 0) {
			// Util.debug("  Last: " + last);
			try {
				numbers.add(Float.parseFloat(last));
			} catch (NumberFormatException nfe) {
				// Just white-space, forget it
			}
			p = s.length();
		}
		return new NumberParser(numbers, p);
	}

	// Process a list of transforms
	// foo(n,n,n...) bar(n,n,n..._ ...)
	// delims are whitespace or ,'s

	private Matrix parseTransform(String s) {
		// Log.d(TAG, s);
		Matrix matrix = new Matrix();
		while (true) {
			parseTransformItem(s, matrix);
			// Log.i(TAG, "Transformed: (" + s + ") " + matrix);
			int rparen = s.indexOf(")");
			if (rparen > 0 && s.length() > rparen + 1) {
				s = s.substring(rparen + 1).replaceFirst("[\\s,]*", "");
			} else {
				break;
			}
		}
		// Log.d(TAG, matrix.toShortString());
		return matrix;
	}

	private Matrix parseTransformItem(String s, Matrix matrix) {
		if (s.startsWith("matrix(")) {
			NumberParser np = parseNumbers(s.substring("matrix(".length()));
			if (np.numbers.size() == 6) {
				Matrix mat = new Matrix();
				mat.setValues(new float[] {
						// Row 1
						np.numbers.get(0), np.numbers.get(2),
						getZoomFactor(np.numbers.get(4)),
						// Row 2
						np.numbers.get(1), np.numbers.get(3),
						getZoomFactor(np.numbers.get(5)),
						// Row 3
						0, 0, 1, });
				matrix.preConcat(mat);
			}
		} else if (s.startsWith("translate(")) {
			NumberParser np = parseNumbers(s.substring("translate(".length()));
			if (np.numbers.size() > 0) {
				float tx = np.numbers.get(0);
				float ty = 0;
				if (np.numbers.size() > 1) {
					ty = np.numbers.get(1);
				}
				matrix.preTranslate(tx, ty);
			}
		} else if (s.startsWith("scale(")) {
			NumberParser np = parseNumbers(s.substring("scale(".length()));
			if (np.numbers.size() > 0) {
				float sx = np.numbers.get(0);
				float sy = sx;
				if (np.numbers.size() > 1) {
					sy = np.numbers.get(1);
				}
				matrix.preScale(sx, sy);
			}
		} else if (s.startsWith("skewX(")) {
			NumberParser np = parseNumbers(s.substring("skewX(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew((float) Math.tan(angle), 0);
			}
		} else if (s.startsWith("skewY(")) {
			NumberParser np = parseNumbers(s.substring("skewY(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew(0, (float) Math.tan(angle));
			}
		} else if (s.startsWith("rotate(")) {
			NumberParser np = parseNumbers(s.substring("rotate(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				float cx = 0;
				float cy = 0;
				if (np.numbers.size() > 2) {
					cx = getZoomFactor(np.numbers.get(1));
					cy = getZoomFactor(np.numbers.get(2));
				}
				matrix.preTranslate(cx, cy);
				matrix.preRotate(angle);
				matrix.preTranslate(-cx, -cy);
			}
		} else {
			Log.i(TAG, "Invalid transform (" + s + ")");
		}
		return matrix;
	}

	void setColorSwap(Integer searchColor, Integer replaceColor) {
		this.searchColor = searchColor;
		this.replaceColor = replaceColor;
	}

	void setWhiteMode(boolean whiteMode) {
		this.whiteMode = whiteMode;
	}

	@Override
	public void startDocument() {
		// Set up prior to parsing a doc
	}

	@Override
	public void endDocument() {
		// Clean up after parsing a doc
		/*
		 * String s = parsed.toString(); if (s.endsWith("</svg>")) { s = s + "";
		 * Log.d(TAG, s); }
		 */
	}

	private boolean doFill(SVGProperties atts) {
		if ("none".equals(atts.getString("display"))) {
			return false;
		}
		if (whiteMode) {
			fillPaint.setShader(null);
			fillPaint.setColor(Color.WHITE);
			return true;
		}
		String fillString = atts.getString("fill");
		if (fillString != null) {
			if (fillString.startsWith("url(#")) {
				// It's a gradient fill, look it up in our map
				String id = fillString.substring("url(#".length(),
						fillString.length() - 1);
				Shader shader = this.gradientMap.get(id);
				if (shader != null) {
					fillPaint.setShader(shader);
					return true;
				} else {
					Log.d(TAG, "Didn't find shader, using black: " + id);
					fillPaint.setShader(null);
					doColor(atts, Color.BLACK, true, fillPaint);
					return true;
				}
			} else if (fillString.equalsIgnoreCase("none")) {
				fillPaint.setShader(null);
				fillPaint.setColor(Color.TRANSPARENT);
				return true;
			} else {
				fillPaint.setShader(null);
				Integer color = atts.getColorValue("fill");
				if (color != null) {
					doColor(atts, color, true, fillPaint);
					return true;
				} else {
					Log.d(TAG, "Unrecognized fill color, using black: "
							+ fillString);
					doColor(atts, Color.BLACK, true, fillPaint);
					return true;
				}
			}
		} else {
			if (fillSet) {
				// If fill is set, inherit from parent
				return fillPaint.getColor() != Color.TRANSPARENT; // optimization
			} else {
				// Default is black fill
				fillPaint.setShader(null);
				fillPaint.setColor(Color.BLACK);
				return true;
			}
		}
	}

	// XXX not done yet
	private boolean doText(Attributes atts, Paint paint) {
		if ("none".equals(atts.getValue("display"))) {
			return false;
		}
		if (atts.getValue("font-size") != null) {
			paint.setTextSize(getZoomFactor(getFloatAttr("font-size", atts, 10f)));
		}
		Typeface typeface = getTypeFace(atts);
		if (typeface != null) {
			paint.setTypeface(typeface);
		}
		Align align = getTextAlign(atts);
		if (align != null) {
			paint.setTextAlign(getTextAlign(atts));
		}
		return true;
	}

	private boolean doStroke(SVGProperties atts) {
		if (whiteMode) {
			// Never stroke in white mode
			return false;
		}
		if ("none".equals(atts.getString("display"))) {
			return false;
		}

		// Check for other stroke attributes
		Float width = getZoomFactor(atts.getFloat("stroke-width"));
		if (width != null) {
			strokePaint.setStrokeWidth(width);
		}

		String linecap = atts.getString("stroke-linecap");
		if ("round".equals(linecap)) {
			strokePaint.setStrokeCap(Paint.Cap.ROUND);
		} else if ("square".equals(linecap)) {
			strokePaint.setStrokeCap(Paint.Cap.SQUARE);
		} else if ("butt".equals(linecap)) {
			strokePaint.setStrokeCap(Paint.Cap.BUTT);
		}

		String linejoin = atts.getString("stroke-linejoin");
		if ("miter".equals(linejoin)) {
			strokePaint.setStrokeJoin(Paint.Join.MITER);
		} else if ("round".equals(linejoin)) {
			strokePaint.setStrokeJoin(Paint.Join.ROUND);
		} else if ("bevel".equals(linejoin)) {
			strokePaint.setStrokeJoin(Paint.Join.BEVEL);
		}

		pathStyleHelper(atts.getString("stroke-dasharray"),
				atts.getString("stroke-dashoffset"));

		String strokeString = atts.getAttr("stroke");
		if (strokeString != null) {
			if (strokeString.equalsIgnoreCase("none")) {
				strokePaint.setColor(Color.TRANSPARENT);
				return false;
			} else {
				Integer color = atts.getColorValue("stroke");
				if (color != null) {
					doColor(atts, color, false, strokePaint);
					return true;
				} else {
					Log.d(TAG, "Unrecognized stroke color, using none: "
							+ strokeString);
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				}
			}
		} else {
			if (strokeSet) {
				// Inherit from parent
				return strokePaint.getColor() != Color.TRANSPARENT; // optimization
			} else {
				// Default is none
				strokePaint.setColor(Color.TRANSPARENT);
				return false;
			}
		}
	}

	private SVGGradient doGradient(boolean isLinear, Attributes atts) {
		SVGGradient gradient = new SVGGradient();
		gradient.id = getStringAttr("id", atts);
		gradient.isLinear = isLinear;
		if (isLinear) {
			gradient.x1 = getZoomFactor(getFloatAttr("x1", atts, 0f));
			gradient.x2 = getZoomFactor(getFloatAttr("x2", atts, 0f));
			gradient.y1 = getZoomFactor(getFloatAttr("y1", atts, 0f));
			gradient.y2 = getZoomFactor(getFloatAttr("y2", atts, 0f));
		} else {
			gradient.x = getZoomFactor(getFloatAttr("cx", atts, 0f));
			gradient.y = getZoomFactor(getFloatAttr("cy", atts, 0f));
			gradient.radius = getZoomFactor(getFloatAttr("r", atts, 0f));
		}
		String transform = getStringAttr("gradientTransform", atts);
		if (transform != null) {
			gradient.matrix = parseTransform(transform);
		}
		String xlink = getStringAttr("href", atts);
		if (xlink != null) {
			if (xlink.startsWith("#")) {
				xlink = xlink.substring(1);
			}
			gradient.xlink = xlink;
		}
		return gradient;
	}

	private void doColor(SVGProperties atts, Integer color, boolean fillMode,
			Paint paint) {
		int c = (0xFFFFFF & color) | 0xFF000000;
		if (searchColor != null && searchColor.intValue() == c) {
			c = replaceColor;
		}
		paint.setColor(c);
		Float opacity = atts.getFloat("opacity");
		if (opacity == null) {
			opacity = atts.getFloat(fillMode ? "fill-opacity"
					: "stroke-opacity");
		}
		if (opacity == null) {
			paint.setAlpha(255);
		} else {
			paint.setAlpha((int) (255 * opacity));
		}
	}

	/**
	 * set the path style (if any) stroke-dasharray="n1,n2,..."
	 * stroke-dashoffset=n
	 */

	private void pathStyleHelper(String style, String offset) {
		if (style == null) {
			return;
		}

		if (style.equals("none")) {
			strokePaint.setPathEffect(null);
			return;
		}

		StringTokenizer st = new StringTokenizer(style, " ,");
		int count = st.countTokens();
		float[] intervals = new float[(count & 1) == 1 ? count * 2 : count];
		float max = 0;
		float current = 1f;
		int i = 0;
		while (st.hasMoreTokens()) {
			intervals[i++] = current = toFloat(st.nextToken(), current);
			max += current;
		}

		// in svg speak, we double the intervals on an odd count
		for (int start = 0; i < intervals.length; i++, start++) {
			max += intervals[i] = intervals[start];
		}

		float off = 0f;
		if (offset != null) {
			try {
				off = Float.parseFloat(offset) % max;
			} catch (NumberFormatException e) {
				// ignore
			}
		}

		strokePaint.setPathEffect(new DashPathEffect(intervals, off));
	}

	private float toFloat(String s, float dflt) {
		float result = dflt;
		try {
			result = Float.parseFloat(s);
		} catch (NumberFormatException e) {
			// ignore
		}
		return result;
	}

	private void doLimits(float x, float y) {
		if (x < limits.left) {
			limits.left = x;
		}
		if (x > limits.right) {
			limits.right = x;
		}
		if (y < limits.top) {
			limits.top = y;
		}
		if (y > limits.bottom) {
			limits.bottom = y;
		}
	}

	private void doLimits(float x, float y, float width, float height) {
		doLimits(x, y);
		doLimits(x + width, y + height);
	}

	private void doLimits(Path path) {
		path.computeBounds(rect, false);
		doLimits(rect.left, rect.top);
		doLimits(rect.right, rect.bottom);
	}

	private final Matrix IDENTITY_MATRIX = new Matrix();

	// XXX could be more selective using save(flags)
	private void pushTransform(Attributes atts) {
		final String transform = getStringAttr("transform", atts);
		final Matrix matrix = transform == null ? IDENTITY_MATRIX
				: parseTransform(transform);
		pushed++;
		canvas.save(); // Canvas.MATRIX_SAVE_FLAG);

		/*
		 * final Matrix m = canvas.getMatrix(); m.postConcat(matrix);
		 * canvas.setMatrix(m);
		 */

		canvas.concat(matrix);
		// Log.d(TAG, "matrix push: " + canvas.getMatrix());
	}

	private void popTransform() {
		canvas.restore();
		// Log.d(TAG, "matrix pop: " + canvas.getMatrix());
		pushed--;
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {

		// System.out.println("Start Element->"+localName);
		// appendElementString(parsed, namespaceURI, localName, qName,
		// atts);

		// Log.d(TAG, localName + showAttributes(atts));
		// Reset paint opacity
		strokePaint.setAlpha(255);
		fillPaint.setAlpha(255);
		// Ignore everything but rectangles in bounds mode
		if (boundsMode) {
			if (localName.equals("rect")) {
				Float x = getFloatAttr("x", atts);
				if (x == null) {
					x = 0f;
				}
				Float y = getFloatAttr("y", atts);
				if (y == null) {
					y = 0f;
				}
				Float width = getFloatAttr("width", atts);
				Float height = getFloatAttr("height", atts);
				bounds = new RectF(x, y, x + width, y + height);
			}
			return;
		}

		// if (inDefsElement) {
		// return;
		// }

		if (localName.equals("svg")) {
			Float width;
			Float height;
			try {
				width = (float) Math.ceil(getFloatAttr("width", atts));
				height = (float) Math.ceil(getFloatAttr("height", atts));

			} catch (Exception e) {
				Log.e(TAG,
						"Height and width not specified in <SVG> tag. Default values (100,100) set.");
				width = height = 100f;
			}
			canvas = picture
					.beginRecording(width.intValue(), height.intValue());

		} else if (localName.equals("defs")) {
			inDefsElement = true;
		} else if (localName.equals("linearGradient")) {
			gradient = doGradient(true, atts);
		} else if (localName.equals("radialGradient")) {
			gradient = doGradient(false, atts);
		} else if (localName.equals("stop")) {
			if (gradient != null) {
				float offset = getFloatAttr("offset", atts);
				String styles = getStringAttr("style", atts);
				int color = Color.BLACK;
				/* sometimes the styles are declared outside */
				if (styles == null) {
					String stopcolor = getStringAttr("stop-color", atts);
					if (stopcolor != null) {
						if (stopcolor.startsWith("#")) {
							color = Integer
									.parseInt(stopcolor.substring(1), 16);
						} else {
							color = SVGColors.mapColor(stopcolor);
						}
					}
				} else {
					/* if the styles are declared in */
					SVGStyleSet styleSet = new SVGStyleSet(styles);
					String colorStyle = styleSet.getStyle("stop-color");
					if (colorStyle != null) {
						if (colorStyle.startsWith("#")) {
							color = Integer.parseInt(colorStyle.substring(1),
									16);
						} else {
							color = Integer.parseInt(colorStyle, 16);
						}
					}
					String opacityStyle = styleSet.getStyle("stop-opacity");
					if (opacityStyle != null) {
						float alpha = Float.parseFloat(opacityStyle);
						int alphaInt = Math.round(255 * alpha);
						color |= (alphaInt << 24);
					} else {
						color |= 0xFF000000;
					}
				}
				gradient.positions.add(offset);
				gradient.colors.add(color);
			}
		} else if (localName.equals("use")) {
			String href = atts.getValue("xlink:href");
			String attTransform = atts.getValue("transform");
			String attX = atts.getValue("x");
			String attY = atts.getValue("y");

			StringBuilder sb = new StringBuilder();
			sb.append("<g");
			sb.append(" xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1'");
			if (attTransform != null || attX != null || attY != null) {
				sb.append(" transform='");
				if (attTransform != null) {
					sb.append(XMLUtils.escapeXMLText(attTransform));
				}
				if (attX != null || attY != null) {
					sb.append("translate(");
					sb.append(attX != null ? XMLUtils.escapeXMLText(attX) : "0");
					sb.append(",");
					sb.append(attY != null ? XMLUtils.escapeXMLText(attY) : "0");
					sb.append(")");
				}
				sb.append("'");
			}

			for (int i = 0; i < atts.getLength(); i++) {
				String attrQName = atts.getQName(i);
				if (!"x".equals(attrQName) && !"y".equals(attrQName)
						&& !"width".equals(attrQName)
						&& !"height".equals(attrQName)
						&& !"xlink:href".equals(attrQName)
						&& !"transform".equals(attrQName)) {

					sb.append(" ");
					sb.append(attrQName);
					sb.append("='");
					sb.append(XMLUtils.escapeXMLText(atts.getValue(i)));
					sb.append("'");
				}
			}

			sb.append(">");

			sb.append(idXml.get(href.substring(1)));

			sb.append("</g>");

			// Log.d(TAG, sb.toString());

			InputSource is = new InputSource(new StringReader(sb.toString()));
			try {
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				xr.setContentHandler(this);
				xr.parse(is);
			} catch (Exception e) {
				Log.d(TAG, sb.toString());
				e.printStackTrace();
			}
		} else if (localName.equals("g")) {
			// Check to see if this is the "bounds" layer
			if ("bounds".equalsIgnoreCase(getStringAttr("id", atts))) {
				boundsMode = true;
			}
			if (hidden) {
				hiddenLevel++;
				// Util.debug("Hidden up: " + hiddenLevel);
			}
			// Go in to hidden mode if display is "none"
			if ("none".equals(getStringAttr("display", atts))) {
				if (!hidden) {
					hidden = true;
					hiddenLevel = 1;
					// Util.debug("Hidden up: " + hiddenLevel);
				}
			}
			pushTransform(atts); // sau
			SVGProperties props = new SVGProperties(atts);

			fillPaintStack.push(new Paint(fillPaint));
			strokePaintStack.push(new Paint(strokePaint));
			fillSetStack.push(fillSet);
			strokeSetStack.push(strokeSet);

			doText(atts, fillPaint);
			doText(atts, strokePaint);
			doFill(props);
			doStroke(props);

			fillSet |= (props.getString("fill") != null);
			strokeSet |= (props.getString("stroke") != null);
		} else if (!hidden && localName.equals("rect")) {
			Float x = getZoomFactor(getFloatAttr("x", atts));
			if (x == null) {
				x = 0f;
			}
			Float y = getZoomFactor(getFloatAttr("y", atts));
			if (y == null) {
				y = 0f;
			}
			Float width = getZoomFactor(getFloatAttr("width", atts));
			Float height = getZoomFactor(getFloatAttr("height", atts));
			Float rx = getZoomFactor(getFloatAttr("rx", atts, 0f));
			Float ry = getZoomFactor(getFloatAttr("ry", atts, 0f));
			pushTransform(atts);
			SVGProperties props = new SVGProperties(atts);
			if (doFill(props)) {
				doLimits(x, y, width, height);
				if (rx <= 0f && ry <= 0f) {
					canvas.drawRect(x, y, x + width, y + height, fillPaint);
				} else {
					rect.set(x, y, x + width, y + height);
					canvas.drawRoundRect(rect, rx, ry, fillPaint);
				}
			}
			if (doStroke(props)) {
				if (rx <= 0f && ry <= 0f) {
					canvas.drawRect(x, y, x + width, y + height, strokePaint);
				} else {
					rect.set(x, y, x + width, y + height);
					canvas.drawRoundRect(rect, rx, ry, strokePaint);
				}
			}
			popTransform();
		} else if (!hidden && localName.equals("line")) {
			Float x1 = getZoomFactor(getFloatAttr("x1", atts));
			Float x2 = getZoomFactor(getFloatAttr("x2", atts));
			Float y1 = getZoomFactor(getFloatAttr("y1", atts));
			Float y2 = getZoomFactor(getFloatAttr("y2", atts));
			SVGProperties props = new SVGProperties(atts);
			if (doStroke(props)) {
				pushTransform(atts);
				doLimits(x1, y1);
				doLimits(x2, y2);
				canvas.drawLine(x1, y1, x2, y2, strokePaint);
				popTransform();
			}
		} else if (!hidden && localName.equals("circle")) {
			Float centerX = getZoomFactor(getFloatAttr("cx", atts));
			Float centerY = getZoomFactor(getFloatAttr("cy", atts));
			Float radius = getZoomFactor(getFloatAttr("r", atts));
			if (centerX != null && centerY != null && radius != null) {
				pushTransform(atts);
				SVGProperties props = new SVGProperties(atts);
				if (doFill(props)) {
					doLimits(centerX - radius, centerY - radius);
					doLimits(centerX + radius, centerY + radius);
					canvas.drawCircle(centerX, centerY, radius, fillPaint);
				}
				if (doStroke(props)) {
					canvas.drawCircle(centerX, centerY, radius, strokePaint);
				}
				popTransform();
			}
		} else if (!hidden && localName.equals("ellipse")) {
			Float centerX = getZoomFactor(getFloatAttr("cx", atts));
			Float centerY = getZoomFactor(getFloatAttr("cy", atts));
			Float radiusX = getZoomFactor(getFloatAttr("rx", atts));
			Float radiusY = getZoomFactor(getFloatAttr("ry", atts));
			if (centerX != null && centerY != null && radiusX != null
					&& radiusY != null) {
				pushTransform(atts);
				SVGProperties props = new SVGProperties(atts);
				rect.set(centerX - radiusX, centerY - radiusY, centerX
						+ radiusX, centerY + radiusY);
				if (doFill(props)) {
					doLimits(centerX - radiusX, centerY - radiusY);
					doLimits(centerX + radiusX, centerY + radiusY);
					canvas.drawOval(rect, fillPaint);
				}
				if (doStroke(props)) {
					canvas.drawOval(rect, strokePaint);
				}
				popTransform();
			}
		} else if (!hidden
				&& (localName.equals("polygon") || localName.equals("polyline"))) {
			NumberParser numbers = getNumberParseAttr("points", atts);
			if (numbers != null) {
				Path p = new Path();
				ArrayList<Float> points = numbers.numbers;
				if (points.size() > 1) {
					pushTransform(atts);
					SVGProperties props = new SVGProperties(atts);
					p.moveTo(points.get(0), points.get(1));
					for (int i = 2; i < points.size(); i += 2) {
						float x = points.get(i);
						float y = points.get(i + 1);
						p.lineTo(x, y);
					}
					// Don't close a polyline
					if (localName.equals("polygon")) {
						p.close();
					}
					if (doFill(props)) {
						doLimits(p);

						// showBounds("fill", p);
						canvas.drawPath(p, fillPaint);
					}
					if (doStroke(props)) {
						// showBounds("stroke", p);
						canvas.drawPath(p, strokePaint);
					}
					popTransform();
				}
			}
		} else if (!hidden && localName.equals("path")) {
			Path p = doPath(getStringAttr("d", atts));
			pushTransform(atts);
			SVGProperties props = new SVGProperties(atts);
			if (doFill(props)) {
				// showBounds("gradient", p);
				doLimits(p);
				// showBounds("gradient", p);
				canvas.drawPath(p, fillPaint);
			}
			if (doStroke(props)) {
				// showBounds("paint", p);
				canvas.drawPath(p, strokePaint);
			}
			popTransform();
		} else if (!hidden && localName.equals("text")) {
			pushTransform(atts);
			text = new SVGText(atts);
		} else if (!hidden) {
			Log.d(TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
		}
	}

	@SuppressWarnings("unused")
	private void showBounds(String text, Path p) {
		RectF b = new RectF();
		p.computeBounds(b, true);
		Log.d(TAG, text + " bounds: " + b.left + "," + b.bottom + " to "
				+ b.right + "," + b.top);
	}

	@SuppressWarnings("unused")
	private String showAttributes(Attributes a) {
		String result = "";
		for (int i = 0; i < a.getLength(); i++) {
			result += " " + a.getLocalName(i) + "='" + a.getValue(i) + "'";
		}
		return result;
	}

	@Override
	public void characters(char ch[], int start, int length) {
		// Log.i(TAG, new String(ch) + " " + start + "/" + length);
		if (text != null) {
			text.setText(ch, start, length);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		// System.out.println("End Element->"+localName+" Qname:"+qName);
		/*
		 * parsed.append("</"); parsed.append(localName); parsed.append(">");
		 */

		// if (inDefsElement) {
		// if (localName.equals("defs")) {
		// inDefsElement = false;
		// }
		// return;
		// }

		if (localName.equals("svg")) {
			picture.endRecording();
			gradientMap.clear();
			gradientRefMap.clear();
		} else if (localName.equals("text")) {
			if (text != null) {
				text.render(canvas);
				text.close();
			}
			popTransform();
		} else if (localName.equals("linearGradient")) {
			if (gradient.id != null) {
				if (gradient.xlink != null) {
					SVGGradient parent = gradientRefMap.get(gradient.xlink);
					if (parent != null) {
						gradient = parent.createChild(gradient);
					}
				}
				int[] colors = new int[gradient.colors.size()];
				for (int i = 0; i < colors.length; i++) {
					colors[i] = gradient.colors.get(i);
				}
				float[] positions = new float[gradient.positions.size()];
				for (int i = 0; i < positions.length; i++) {
					positions[i] = gradient.positions.get(i);
				}
				if (colors.length == 0) {
					Log.e(TAG, "No Gradient colors for " + gradient.id);
				}
				LinearGradient g = new LinearGradient(gradient.x1, gradient.y1,
						gradient.x2, gradient.y2, colors, positions,
						Shader.TileMode.CLAMP);
				if (gradient.matrix != null) {
					g.setLocalMatrix(gradient.matrix);
				}
				gradientMap.put(gradient.id, g);
				gradientRefMap.put(gradient.id, gradient);
			}
		} else if (localName.equals("radialGradient")) {
			if (gradient.id != null) {
				int[] colors = new int[gradient.colors.size()];
				for (int i = 0; i < colors.length; i++) {
					colors[i] = gradient.colors.get(i);
				}
				float[] positions = new float[gradient.positions.size()];
				for (int i = 0; i < positions.length; i++) {
					positions[i] = gradient.positions.get(i);
				}
				if (gradient.xlink != null) {
					SVGGradient parent = gradientRefMap.get(gradient.xlink);
					if (parent != null) {
						gradient = parent.createChild(gradient);
					}
				}
				RadialGradient g = new RadialGradient(gradient.x, gradient.y,
						gradient.radius, colors, positions,
						Shader.TileMode.CLAMP);
				if (gradient.matrix != null) {
					g.setLocalMatrix(gradient.matrix);
				}
				gradientMap.put(gradient.id, g);
				gradientRefMap.put(gradient.id, gradient);
			}
		} else if (localName.equals("g")) {
			if (boundsMode) {
				boundsMode = false;
			}
			// Break out of hidden mode
			if (hidden) {
				hiddenLevel--;
				// Util.debug("Hidden down: " + hiddenLevel);
				if (hiddenLevel == 0) {
					hidden = false;
				}
			}
			// Clear gradient map
			// gradientMap.clear();
			popTransform(); // SAU
			fillPaint = fillPaintStack.pop();
			fillSet = fillSetStack.pop();
			strokePaint = strokePaintStack.pop();
			strokeSet = strokeSetStack.pop();
		}
	}

	// class to hold text properties

	private final class SVGText {
		private final static int MIDDLE = 1;
		private final static int TOP = 2;
		private Paint stroke = null, fill = null;
		private float x, y;
		private String svgText;
		private boolean inText;
		private int vAlign = 0;

		public SVGText(Attributes atts) {
			// Log.d(TAG, "text");
			x = getZoomFactor(getFloatAttr("x", atts, 0f));
			y = getZoomFactor(getFloatAttr("y", atts, 0f));
			svgText = null;
			inText = true;

			SVGProperties props = new SVGProperties(atts);
			if (doFill(props)) {
				fill = new Paint(fillPaint);
				doText(atts, fill);
			}
			if (doStroke(props)) {
				stroke = new Paint(strokePaint);
				doText(atts, stroke);
			}
			// quick hack
			String valign = getStringAttr("alignment-baseline", atts);
			if ("middle".equals(valign)) {
				vAlign = MIDDLE;
			} else if ("top".equals(valign)) {
				vAlign = TOP;
			}
		}

		// ignore tspan elements for now
		public void setText(char[] ch, int start, int len) {
			if (isInText()) {
				if (svgText == null) {
					svgText = new String(ch, start, len);
				} else {
					svgText += new String(ch, start, len);
				}

				// This is an experiment for vertical alignment
				if (vAlign > 0) {
					Paint paint = stroke == null ? fill : stroke;
					Rect bnds = new Rect();
					paint.getTextBounds(svgText, 0, svgText.length(), bnds);
					// Log.i(TAG, "Adjusting " + y + " by " + bnds);
					y += (vAlign == MIDDLE) ? -bnds.centerY() : bnds.height();
				}
			}
		}

		public boolean isInText() {
			return inText;
		}

		public void close() {
			inText = false;
		}

		public void render(Canvas canvas) {
			if (fill != null) {
				canvas.drawText(svgText, x, y, fill);
			}
			if (stroke != null) {
				canvas.drawText(svgText, x, y, stroke);
			}
			// Log.i(TAG, "Drawing: " + svgText + " " + x + "," + y);
		}
	}

	private static final class SVGGradient {
		private String id;
		private String xlink;
		private boolean isLinear;
		private float x1, y1, x2, y2;
		private float x, y, radius;
		private ArrayList<Float> positions = new ArrayList<Float>();
		private ArrayList<Integer> colors = new ArrayList<Integer>();
		private Matrix matrix = null;

		@Override
		public String toString() {
			return "Gradient [id=" + id + ", isLinear=" + isLinear + "]";
		}

		SVGGradient createChild(SVGGradient g) {
			SVGGradient child = new SVGGradient();
			child.id = g.id;
			child.xlink = id;
			child.isLinear = g.isLinear;
			child.x1 = g.x1;
			child.x2 = g.x2;
			child.y1 = g.y1;
			child.y2 = g.y2;
			child.x = g.x;
			child.y = g.y;
			child.radius = g.radius;
			child.positions = positions;
			child.colors = colors;
			child.matrix = matrix;
			if (g.matrix != null) {
				if (matrix == null) {
					child.matrix = g.matrix;
				} else {
					Matrix m = new Matrix(matrix);
					m.preConcat(g.matrix);
					child.matrix = m;
				}
			}
			return child;
		}
	}

	private static final class SVGStyleSet {
		private HashMap<String, String> styleMap = new HashMap<String, String>();

		private SVGStyleSet(String string) {
			String[] styles = string.split(";");
			for (String s : styles) {
				String[] style = s.split(":");
				if (style.length == 2) {
					styleMap.put(style[0], style[1]);
				}
			}
		}

		private String getStyle(String name) {
			return styleMap.get(name);
		}
	}

	private static final class NumberParser {
		private ArrayList<Float> numbers;
		private int nextCmd;

		private NumberParser(ArrayList<Float> numbers, int nextCmd) {
			this.numbers = numbers;
			this.nextCmd = nextCmd;
		}

		@SuppressWarnings("unused")
		private int getNextCmd() {
			return nextCmd;
		}

		@SuppressWarnings("unused")
		private float getNumber(int index) {
			return numbers.get(index);
		}

	}

	private final class SVGProperties {
		private SVGStyleSet styles = null;
		private Attributes atts;

		private SVGProperties(Attributes atts) {
			this.atts = atts;
			String styleAttr = getStringAttr("style", atts);
			if (styleAttr != null) {
				styles = new SVGStyleSet(styleAttr);
			}
		}

		private String getAttr(String name) {
			String v = null;
			if (styles != null) {
				v = styles.getStyle(name);
			}
			if (v == null) {
				v = getStringAttr(name, atts);
			}
			return v;
		}

		private String getString(String name) {
			return getAttr(name);
		}

		private Integer getColorValue(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
				/* color is represented as #RRGGBB */
			} else if (v.startsWith("#")
					&& (v.length() == 4 || v.length() == 7)) {
				try {
					int result = Integer.parseInt(v.substring(1), 16);
					return v.length() == 4 ? ColorUtils.hex3Tohex6(result)
							: result;
				} catch (NumberFormatException nfe) {
					return null;
				}
				/* Color is represented as a name */
			} else {
				return SVGColors.mapColor(v);
			}
		}

		@SuppressWarnings("unused")
		private Float getFloat(String name, float defaultValue) {
			Float v = getFloat(name);
			if (v == null) {
				return defaultValue;
			} else {
				return v;
			}
		}

		private Float getFloat(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
			} else {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException nfe) {
					return null;
				}
			}
		}
	}

	private String getStringAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return attributes.getValue(i);
			}
		}
		return null;
	}

	private Float getFloatAttr(String name, Attributes attributes) {
		return getFloatAttr(name, attributes, null);
	}

	private Float getFloatAttr(String name, Attributes attributes,
			Float defaultValue) {
		String v = getStringAttr(name, attributes);
		if (v == null) {
			return defaultValue;
		} else {
			if (v.endsWith("px")) {
				v = v.substring(0, v.length() - 2);
			} else if (v.endsWith("pt")) {
				v = v.substring(0, v.length() - 2);
				float value = Float.parseFloat(v);
				/* convert points to pixels */
				return (value * 96) / 72;
			}

			// Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
			return Float.parseFloat(v);
		}
	}

	private Align getTextAlign(Attributes atts) {
		String align = getStringAttr("text-anchor", atts);
		if (align == null) {
			return null;
		}
		if ("middle".equals(align)) {
			return Align.CENTER;
		} else if ("end".equals(align)) {
			return Align.RIGHT;
		} else {
			return Align.LEFT;
		}
	}

	private Typeface getTypeFace(Attributes atts) {
		String face = getStringAttr("font-family", atts);
		String style = getStringAttr("font-style", atts);
		String weight = getStringAttr("font-weight", atts);

		if (face == null && style == null && weight == null) {
			return null;
		}
		int styleParam = Typeface.NORMAL;
		if ("italic".equals(style)) {
			styleParam |= Typeface.ITALIC;
		}
		if ("bold".equals(weight)) {
			styleParam |= Typeface.BOLD;
		}
		Typeface result = Typeface.create(face, styleParam);
		// Log.d(TAG, "typeface=" + result + " " + styleParam);
		return result;
	}
}
