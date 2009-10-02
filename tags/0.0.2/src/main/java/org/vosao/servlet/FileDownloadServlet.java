/**
 * Vosao CMS. Simple CMS for Google App Engine.
 * Copyright (C) 2009 Vosao development team
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * email: vosao.dev@gmail.com
 */

package org.vosao.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.business.decorators.TreeItemDecorator;
import org.vosao.entity.FileEntity;
import org.vosao.entity.FolderEntity;
import org.vosao.utils.DateUtil;

/**
 * Servlet for download files from database.
 * 
 * @author Aleksandr Oleynik
 */
public class FileDownloadServlet extends BaseSpringServlet {
	
	private static final long CACHE_LIMIT = 1048576;
	
	private static final long serialVersionUID = 6098745782027999297L;
	private static final Log log = LogFactory.getLog(FileDownloadServlet.class);

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

        log.info("get file " + request.getPathInfo());
		String[] chain = FolderUtil.getPathChain(request.getPathInfo());
		if (chain.length == 0) {
			response.getWriter().append("file was not specified");
			return;
		}
		
		if (isInCache(request.getPathInfo())) {
			sendFromCache(request, response);
			return;
		}
		
		String filename = chain[chain.length-1];
		
		TreeItemDecorator<FolderEntity> tree = getBusiness().getFolderBusiness()
				.getTree();
		TreeItemDecorator<FolderEntity> folder = getBusiness().getFolderBusiness()
				.findFolderByPath(tree, FolderUtil.getFilePath(
						request.getPathInfo()));
		if (folder == null) {
	        log.info("folder " + request.getPathInfo() + " was not found");
			response.getWriter().append("folder " + request.getPathInfo() 
					+ " was not found");
			return;
		}
		FileEntity file = getDao().getFileDao().getByName(
				folder.getEntity().getId(), filename); 
		if (file != null) {
			if (file.getSize() < CACHE_LIMIT) {
				getBusiness().getCache().put(request.getPathInfo(), file);
			}
			sendFile(file, request, response);
		}
		else {
	        log.info("file " + request.getPathInfo() + " was not found");
			response.getWriter().append("file " + request.getPathInfo() 
					+ " was not found");
		}
	}
	
	private boolean isInCache(final String path) {
		return getBusiness().getCache().containsKey(path);
	}
	
	private void sendFromCache(HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
        log.info("taking from memcache " + request.getPathInfo());
		FileEntity file = (FileEntity) getBusiness().getCache().get(
				request.getPathInfo());
		sendFile(file, request, response);
	}
	
	private void sendFile(final FileEntity file, HttpServletRequest request,
			HttpServletResponse response) 
			throws IOException {
		if(DateUtil.toHeaderString(file.getLastModifiedTime()).equals(
				request.getHeader("If-Modified-Since"))){
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		else {
			response.setHeader("Content-type", file.getMimeType());
			
			response.setHeader("Content-Length", String.valueOf(file.getSize()));
			response.setHeader("Last-Modified", 
					DateUtil.toHeaderString(file.getLastModifiedTime()));
			BufferedOutputStream output = new BufferedOutputStream(
					response.getOutputStream());
			output.write(getDao().getFileDao().getFileContent(file));
			output.flush();
			output.close();
		}
	}
	
}