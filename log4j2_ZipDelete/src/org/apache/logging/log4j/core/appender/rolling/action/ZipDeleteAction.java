package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.appender.rolling.PatternProcessor;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Rollover or scheduled action for deleting old log files that are accepted by the specified PathFilters.
 */
@Plugin(name = "ZipDelete", category = "Core", printObject = true)
public class ZipDeleteAction extends DeleteAction {
    
    private final StrSubstitutor substitutor;
    private final PatternProcessor patternProcessor;
    
    

    ZipDeleteAction(final String basePath, final boolean followSymbolicLinks, final int maxDepth, final boolean testMode,
            final PathSorter sorter, final PathCondition[] pathConditions, final ScriptCondition scriptCondition,
            final StrSubstitutor subst, String zipPattern) {
        super(basePath, followSymbolicLinks, maxDepth, testMode, sorter, pathConditions, scriptCondition, subst);
        this.substitutor=getStrSubstitutor();
        this.patternProcessor=new PatternProcessor(zipPattern);
    }


    private boolean shouldDelete(PathWithAttributes element) {
    	Path basePath = getBasePath();
        for (final PathCondition pathFilter : getPathConditions()) {
            final Path relative = basePath.relativize(element.getPath());
            if(!relative.toFile().getName().endsWith(".log") ||
              (!pathFilter.accept(basePath, relative, element.getAttributes()))) {
                return false;
            }
        }
        return true;
    }
    
    private List<PathWithAttributes> afterConditions(List<PathWithAttributes> sortedPaths) {
    	ArrayList<PathWithAttributes> res =  new ArrayList<PathWithAttributes>();
    	for (final PathWithAttributes element : sortedPaths) {
    		if(shouldDelete(element)) {
    			res.add(element);
    		}
    	}
    	return res;
    }
    
    private File getFileFor(StringBuilder buf, int i) {
    	buf.setLength(0);
    	patternProcessor.formatFileName(substitutor, buf, i);
        String tmpStr = substitutor.replace(buf);
        return new File(tmpStr);
    }
    
	private File getZipFilename() {
		final StringBuilder buf = new StringBuilder();
		for(int i = 0; i <10 ; i++) {
	        File tmpFile = getFileFor(buf, i);
	        if(!tmpFile.exists())
	        	return tmpFile;
		}
		File res = getFileFor(buf, 0);
		LOGGER.warn("no more slots for, returning first one: " + res.getAbsolutePath());
		return res;
	}
	
    private void trace(final String label, final List<PathWithAttributes> sortedPaths) {
        LOGGER.trace(label);
        for (final PathWithAttributes pathWithAttributes : sortedPaths) {
            LOGGER.trace(pathWithAttributes);
        }
    }
	
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.appender.rolling.action.AbstractPathAction#execute(FileVisitor)
     */
    @Override
    public boolean execute(final FileVisitor<Path> visitor) throws IOException {
        final List<PathWithAttributes> sortedPaths = afterConditions(getSortedPaths());
        trace("Sorted paths after conditions:", sortedPaths);

        File zipFile = getZipFilename();
        
        /**
         * zip all this
         */
        Zipper zipper = new Zipper(zipFile);
        for (final PathWithAttributes element : sortedPaths) {
        	zipper.addFileToZip(element.getPath().toFile());
        }
        if(!sortedPaths.isEmpty()) {
        	LOGGER.trace("zipping to: " + zipFile.getAbsolutePath());
        	zipper.zip();
        }
        
        /**
         * delete
         */
        for (final PathWithAttributes element : sortedPaths) {
    		try {
                visitor.visitFile(element.getPath(), element.getAttributes());
            } catch (final IOException ioex) {
                LOGGER.error("Error in post-rollover Delete when visiting {}", element.getPath(), ioex);
                visitor.visitFileFailed(element.getPath(), ioex);
            }
        }
        
        return true; // do not abort rollover even if processing failed
    }

    @Override
    protected FileVisitor<Path> createFileVisitor(final Path visitorBaseDir, final List<PathCondition> conditions) {
    	/**
    	 * lets instantiate it without conditions, we will check for the conditions in our action
    	 */
        return new DeletingVisitor(visitorBaseDir, new ArrayList<PathCondition>(0), super.isTestMode());
    }

    
    
    @PluginFactory
    public static ZipDeleteAction createDeleteAction(
            // @formatter:off
            @PluginAttribute("basePath") final String basePath, //
            @PluginAttribute(value = "followLinks", defaultBoolean = false) final boolean followLinks,
            @PluginAttribute(value = "maxDepth", defaultInt = 1) final int maxDepth,
            @PluginAttribute(value = "testMode", defaultBoolean = false) final boolean testMode,
            @PluginElement("PathSorter") final PathSorter sorterParameter,
            @PluginElement("PathConditions") final PathCondition[] pathConditions,
            @PluginElement("ScriptCondition") final ScriptCondition scriptCondition,
            @PluginConfiguration final Configuration config,
            //mine
            @PluginAttribute("zipPattern") final String zipPattern
    		) {
            // @formatter:on
        final PathSorter sorter = sorterParameter == null ? new PathSortByModificationTime(true) : sorterParameter;
        return new ZipDeleteAction(basePath, followLinks, maxDepth, testMode, sorter, pathConditions, scriptCondition,
                config.getStrSubstitutor(), zipPattern);
    }
}
