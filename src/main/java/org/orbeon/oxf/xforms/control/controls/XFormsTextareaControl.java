/**
 * Copyright (C) 2009 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.control.controls;

import org.dom4j.Document;
import org.dom4j.Element;
import org.orbeon.oxf.util.IndentedLogger;
import org.orbeon.oxf.xforms.XFormsConstants;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.control.XFormsControl;
import org.orbeon.oxf.xforms.control.XFormsValueFocusableControlBase;
import org.orbeon.oxf.xforms.xbl.XBLContainer;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import scala.Tuple3;

/**
 * Represents an xf:textarea control.
 */
public class XFormsTextareaControl extends XFormsValueFocusableControlBase { // TODO: move to Scala

    public XFormsTextareaControl(XBLContainer container, XFormsControl parent, Element element, String id) {
        super(container, parent, element, id);
    }

    @Override
    public Tuple3<String, String, String> getJavaScriptInitialization() {
        final boolean hasInitialization = XFormsControl.isHTMLMediatype(this) || getAppearances().contains(XFormsConstants.XXFORMS_AUTOSIZE_APPEARANCE_QNAME);

        return hasInitialization ? getCommonJavaScriptInitialization() : null;
    }

    /**
     * For textareas with mediatype="text/html", we first clean the HTML with TagSoup, and then transform it with
     * a stylesheet that removes all unknown or dangerous content.
     */
    @Override
    public String translateExternalValue(String externalValue) {
        if (XFormsControl.isHTMLMediatype(this)) {
            final IndentedLogger indentedLogger = containingDocument().getControls().getIndentedLogger();
            final boolean isDebugEnabled = indentedLogger.isDebugEnabled();
            if (isDebugEnabled)
                indentedLogger.startHandleOperation("xf:textarea", "cleaning-up HTML", "value", externalValue);

            // Do TagSoup and XSLT cleaning
            final Document tagSoupedDocument = XFormsUtils.htmlStringToDom4jTagSoup(externalValue, null);
            if (isDebugEnabled)
                indentedLogger.logDebug("xf:textarea", "after TagSoup cleanup", "value", Dom4jUtils.domToString(tagSoupedDocument));
            final Document cleanedDocument = XMLUtils.cleanXML(tagSoupedDocument, "oxf:/ops/xforms/clean-html.xsl");

            // Remove dummy tags (the dummy tags are added by the XSLT, as we need a root element for XSLT processing)
            externalValue = Dom4jUtils.domToString(cleanedDocument);
            if ("<dummy-root/>".equals(externalValue)) {
                externalValue = "";                                                                 // Becomes empty
            } else {
                externalValue = externalValue.substring("<dummy-root>".length());                           // Remove start dummy tag
                externalValue = externalValue.substring(0, externalValue.length() - "</dummy-root>".length());      // Remove end dummy tag
            }
            if (isDebugEnabled)
                indentedLogger.endHandleOperation("value", externalValue);
        }

        // String replacement based on configuration
        externalValue = containingDocument().getStaticState().sanitizeTextarea().apply(externalValue);

        return externalValue;
    }
}