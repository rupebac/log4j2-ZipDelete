# log4j2-ZipDelete
An implementation of a delete action for log4j2, that zips the content that is about to be deleted.

There seems to be no way of rotating a log file like app_1.log, app_2.log, and at the same time archive old rotated files in a zip file. This is overriding the Delete action to Zip all the content before.

Use example:

		  <DefaultRolloverStrategy fileIndex="min">
			<ZipDelete basePath="${baseDir}" maxDepth="1" zipPattern=".logs/app_%d{yyyy-MM-dd}_%i.zip">
				<IfLastModified age="7D" />
				<IfFileName glob=".logs/app*.log" />
			</ZipDelete>
		  </DefaultRolloverStrategy>
