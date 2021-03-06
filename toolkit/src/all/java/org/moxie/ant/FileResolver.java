/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000, 2001, 2002, 2003 Jesse Stockall.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 */
package org.moxie.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a directory in the classpath.
 * <p>
 * 
 * When a directory is located in the classpath, a FileResolver is instantiated
 * that encapsulates the path and performs searches in that directory. This
 * class is used primarily to allow easy association of the <i>source directory
 * </i> with the jar entry's attributes.
 * <p>
 * 
 * When a file is resolved from a JarEntrySpec, Attributes are added for the
 * source file's path and last modification time.
 * <p>
 * 
 * 
 * 
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler
 *         </a>
 * @author Jesse Stockall
 * @version $Revision: 1.2 $ $Date: 2003/02/23 10:06:10 $
 */
class FileResolver extends PathResolver {
	/** the 'base' directory as specified in the classpath */
	File base = null;

	/**
	 * constructs a new FileResolver using the given <i>base directory</i>
	 * 
	 * @param base
	 *            a directory at which file searches begin
	 * @param log
	 *            an ant logging mechanism
	 */
	FileResolver(File base, Logger log) {
		super(false, log);

		this.base = base;
		log.verbose("Resolver: " + base);
	}

	/**
	 * Nothing to close
	 * 
	 * @throws IOException
	 *             Oops!
	 */
	public void close() throws IOException {
		// nothing to close
	}

	/**
	 * Resolve the file specified in a JarEntrySpec to a stream.
	 * 
	 * @param spec
	 *            the JarEntrySpec to resolve
	 * @return an InputStream open on the resolved file or null
	 * @exception IOException
	 *                if opening the stream fails
	 */
	public InputStream resolve(JarEntrySpec spec) throws IOException {
		InputStream is = null;
		File f;

		//
		// if the entrySpec already has a source file
		// associated, use it otherwise attempt to
		// create one
		//
		if ((f = spec.getSourceFile()) == null) {
			f = new File(base, spec.getJarName());
		}

		if (f.exists()) {
			spec.setSourceFile(f);
			is = new FileInputStream(f);
			log.debug(spec.getJarName() + "->" + base);
		}
		return is;
	}

	/**
	 * Description of the Method
	 * 
	 * @param fname
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @throws IOException
	 *             Description of the Exception
	 */
	public InputStream resolve(String fname) throws IOException {
		InputStream is = null;
		File f = new File(base, fname);

		if (f.exists()) {
			is = new FileInputStream(f);
			log.debug(fname + "->" + base);
		}
		return is;
	}
	
	@Override
	public String toString() {
		return "Resolver " + (isExcluded() ? "EXCLUDED" : "") + " (" + base.getPath() + ")";
	}

}