/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi2.xslf.usermodel;

import org.apache.poi2.util.POILogFactory;
import org.apache.poi2.util.POILogger;
import org.apache.poi2.POIXMLDocumentPart;
import org.apache.poi2.POIXMLException;
import org.apache.poi2.POIXMLFactory;
import org.apache.poi2.POIXMLRelation;
import org.apache.poi2.openxml4j.opc.PackagePart;
import org.apache.poi2.openxml4j.opc.PackageRelationship;
import org.apache.poi2.util.Beta;

import java.lang.reflect.Constructor;

/**
 * Instantiates sub-classes of POIXMLDocumentPart depending on their relationship type
 *
 * @author Yegor Kozlov
 */
@Beta
public final class XSLFFactory extends POIXMLFactory {
    private static final POILogger logger = POILogFactory.getLogger(XSLFFactory.class);

    private XSLFFactory(){

    }

    private static final XSLFFactory inst = new XSLFFactory();

    public static XSLFFactory getInstance(){
        return inst;
    }

    @Override
    public org.apache.poi2.POIXMLDocumentPart createDocumentPart(org.apache.poi2.POIXMLDocumentPart parent, org.apache.poi2.openxml4j.opc.PackageRelationship rel, org.apache.poi2.openxml4j.opc.PackagePart part){
        org.apache.poi2.POIXMLRelation descriptor = XSLFRelation.getInstance(rel.getRelationshipType());
        if(descriptor == null || descriptor.getRelationClass() == null){
            logger.log(POILogger.DEBUG, "using default POIXMLDocumentPart for " + rel.getRelationshipType());
            return new org.apache.poi2.POIXMLDocumentPart(part, rel);
        }

        try {
            Class<? extends org.apache.poi2.POIXMLDocumentPart> cls = descriptor.getRelationClass();
            Constructor<? extends org.apache.poi2.POIXMLDocumentPart> constructor = cls.getDeclaredConstructor(PackagePart.class, PackageRelationship.class);
            return constructor.newInstance(part, rel);
        } catch (Exception e){
            throw new org.apache.poi2.POIXMLException(e);
        }
    }

    @Override
    public org.apache.poi2.POIXMLDocumentPart newDocumentPart(POIXMLRelation descriptor){
        try {
            Class<? extends org.apache.poi2.POIXMLDocumentPart> cls = descriptor.getRelationClass();
            Constructor<? extends POIXMLDocumentPart> constructor = cls.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (Exception e){
            throw new POIXMLException(e);
        }
    }

}
