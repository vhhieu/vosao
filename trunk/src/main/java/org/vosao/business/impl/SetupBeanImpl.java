package org.vosao.business.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.cache.CacheStatistics;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.business.Business;
import org.vosao.business.SetupBean;
import org.vosao.dao.Dao;
import org.vosao.entity.FolderEntity;
import org.vosao.entity.PageEntity;
import org.vosao.entity.TemplateEntity;
import org.vosao.entity.UserEntity;
import org.vosao.enums.UserRole;
import org.vosao.jsf.JSFUtil;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

public class SetupBeanImpl implements SetupBean {

	private static Log log = LogFactory.getLog(SetupBeanImpl.class);

	private Dao dao;
	private Business business;
	
	public void setup() {
		log.info("setup...");
		clearSessions();
		try {
			clearCache();
		} catch (CacheException e) {
			log.error(e);
		}
		initUsers();
		initTemplates();
		initPages();
		initFolders();
		initConfigs();
	}
	
	private void clearSessions() {
        DatastoreService datastore = DatastoreServiceFactory
        		.getDatastoreService();
        Query query = new Query("_ah_SESSION");
        PreparedQuery results = datastore.prepare(query);
        log.info("Deleting " + results.countEntities() + " sessions from data store");
        for (Entity session : results.asIterable()) {
            datastore.delete(session.getKey());
        }
	} 
	
	private void clearCache() throws CacheException {
        CacheFactory cacheFactory = CacheManager.getInstance()
        		.getCacheFactory();
        Cache cache = cacheFactory.createCache(Collections.emptyMap());
        CacheStatistics stats = cache.getCacheStatistics();
        log.info("Clearing " + stats.getObjectCount() + " objects in cache");
        cache.clear();
	} 

	private void initUsers() {
		List<UserEntity> admins = getDao().getUserDao().getByRole(UserRole.ADMIN);
		if (admins.size() == 0) {
			UserEntity admin = new UserEntity("admin", "admin", "admin@test.com", 
					UserRole.ADMIN);
			getDao().getUserDao().save(admin);
	        log.info("Adding admin user admin@test.com.");
		}
	}

	private void initPages() {
		List<PageEntity> roots = getDao().getPageDao().getByParent(null);
		if (roots.size() == 0) {
			String content = "Hello!";
			try {
				content = JSFUtil.getTextResource("org/vosao/resources/html/root.html");
			}
			catch(IOException e) {
		        log.error("Can't read default root page." + e);
			}
			TemplateEntity template = getDao().getTemplateDao().getByUrl("simple");
			PageEntity root = new PageEntity("root", content, "/", null, 
					template.getId());
			getDao().getPageDao().save(root);
	        log.info("Adding root page.");
		}
	}

	private void initTemplates() {
		List<TemplateEntity> list = getDao().getTemplateDao().select();
		if (list.size() == 0) {
			String content = "$page.content";
			try {
				content = JSFUtil.getTextResource("org/vosao/resources/html/simple.html");
			}
			catch(IOException e) {
		        log.error("Can't read default template." + e);
			}
			TemplateEntity template = new TemplateEntity("Simple", content, "simple");
			getDao().getTemplateDao().save(template);
	        log.info("Adding default template.");
		}
	}

	private void initFolders() {
		List<FolderEntity> roots = getDao().getFolderDao().getByParent(null);
		if (roots.size() == 0) {
	        log.info("Adding default folders.");
			FolderEntity root = new FolderEntity("/", null);
			getDao().getFolderDao().save(root);
			FolderEntity theme = new FolderEntity("Themes resources", "theme", root.getId());
			getDao().getFolderDao().save(theme);
			FolderEntity simple = new FolderEntity("Simple", "simple", theme.getId());
			getDao().getFolderDao().save(simple);
		}
	}
	
	public Dao getDao() {
		return dao;
	}

	public void setDao(Dao dao) {
		this.dao = dao;
	}

	public Business getBusiness() {
		return business;
	}

	public void setBusiness(Business business) {
		this.business = business;
	}

	private void initConfigs() {
		if (StringUtils.isEmpty(getBusiness().getConfigBusiness()
				.getGoogleAnalyticsId())) {
	        log.info("Adding google analytic config.");
	        getBusiness().getConfigBusiness().setGoogleAnalyticsId("");
		}
	}
	
}