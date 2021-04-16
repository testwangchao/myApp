### Android使用jacoco来统计功能测试覆盖率

#### 多module

【gradle使用的是6】

1. app下添加test文件夹

   ![image-20210416144153869](/Users/happyelements/Library/Application Support/typora-user-images/image-20210416144153869.png)

   FinishListener.class

   ```java
   package com.example.test;
   
   public interface FinishListener {
       void onActivityFinished();
       void dumpIntermediateCoverage(String filePath);
   }
   ```

   InstrumentedActivity.class

   ```java
   package com.example.test;
   
   import com.example.myfirstapp.MainActivity;
   
   public class InstrumentedActivity extends MainActivity {
       public static String TAG = "IntrumentedActivity";
       private com.example.test.FinishListener mListener;
       public void setFinishListener(com.example.test.FinishListener listener) {
           mListener = listener;
       }
       @Override
       public void onDestroy() {
           super.onDestroy();
           super.finish();
           if (mListener != null) {
               mListener.onActivityFinished();
           }
       }
   }
   ```

   JacocoInstrumentation.class

   ```java
   package com.example.test;
   
   import android.app.Activity;
   import android.app.Instrumentation;
   import android.content.Intent;
   import android.os.Bundle;
   import android.os.Looper;
   import android.util.Log;
   
   import java.io.File;
   import java.io.FileOutputStream;
   import java.io.IOException;
   import java.io.OutputStream;
   
   public class JacocoInstrumentation extends Instrumentation implements com.example.test.FinishListener {
       public static String TAG = "JacocoInstrumentation:";
       private static String DEFAULT_COVERAGE_FILE_PATH = "/mnt/sdcard/coverage.ec";
       private final Bundle mResults = new Bundle();
       private Intent mIntent;
       private static final boolean LOGD = true;
       private boolean mCoverage = true;
       private String mCoverageFilePath;
   
       public JacocoInstrumentation() {
       }
   
       @Override
       public void onCreate(Bundle arguments) {
           Log.d(TAG, "onCreate(" + arguments + ")");
           super.onCreate(arguments);
           DEFAULT_COVERAGE_FILE_PATH = getContext().getFilesDir().getPath().toString() + "/coverage.ec";
           File file = new File(DEFAULT_COVERAGE_FILE_PATH);
           if(!file.exists()){
               try{
                   file.createNewFile();
               }catch (IOException e){
                   Log.d(TAG,"新建文件异常："+e);
                   e.printStackTrace();}
           }
           if(arguments != null) {
               mCoverageFilePath = arguments.getString("coverageFile");
           }
           mIntent = new Intent(getTargetContext(), InstrumentedActivity.class);
           mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           start();
       }
   
       @Override
       public void onStart() {
           super.onStart();
           Looper.prepare();
           InstrumentedActivity activity = (InstrumentedActivity) startActivitySync(mIntent);
           activity.setFinishListener(this);
       }
   
       private String getCoverageFilePath() {
           if (mCoverageFilePath == null) {
               return DEFAULT_COVERAGE_FILE_PATH;
           } else {
               return mCoverageFilePath;
           }
       }
   
       private void generateCoverageReport() {
           Log.d(TAG, "generateCoverageReport():" + getCoverageFilePath());
           OutputStream out = null;
           try {
               out = new FileOutputStream(getCoverageFilePath(), false);
               Object agent = Class.forName("org.jacoco.agent.rt.RT")
                       .getMethod("getAgent")
                       .invoke(null);
               out.write((byte[]) agent.getClass().getMethod("getExecutionData", boolean.class)
                       .invoke(agent, false));
           } catch (Exception e) {
               Log.d(TAG, "getExecutionDataError");
               Log.d(TAG, e.toString(), e);
           } finally {
               if (out != null) {
                   try {
                       out.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }
       }
   
       @Override
       public void onActivityFinished() {
           if (LOGD)      Log.d(TAG, "onActivityFinished()");
           if (mCoverage) {
               Log.d(TAG, "开始写入");
               generateCoverageReport();
           }
           finish(Activity.RESULT_OK, mResults);
       }
   
       @Override
       public void dumpIntermediateCoverage(String filePath) {
   
       }
   }
   ```

2. AndroidManifest.xml

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       package="com.example.myfirstapp">
   
       <application
           android:allowBackup="true"
           android:icon="@mipmap/ic_launcher"
           android:label="@string/app_name"
           android:roundIcon="@mipmap/ic_launcher_round"
           android:supportsRtl="true"
           android:theme="@style/Theme.AppCompat.Light.NoActionBar">
           <activity android:name=".DisplayMessageActivity"
               android:parentActivityName=".MainActivity">
               <!-- The meta-data tag is required if you support API level 15 and lower -->
               <meta-data
                   android:name="android.support.PARENT_ACTIVITY"
                   android:value=".MainActivity" />
           </activity>
           <activity android:name=".MainActivity">
               <intent-filter>
                   <action android:name="android.intent.action.MAIN" />
   
                   <category android:name="android.intent.category.LAUNCHER" />
               </intent-filter>
           </activity>
           <!--添加InstrumentationActivity-->
           <activity
               android:label="InstrumentationActivity"
               android:name="com.example.test.InstrumentedActivity" />
       </application>
       <!--
       android:name JacocoInstrumentation的位置
       android:targetPackage=包名
       -->
       <instrumentation
           android:handleProfiling="true"
           android:label="CoverageInstrumentation"
           android:name="com.example.test.JacocoInstrumentation"
           android:targetPackage="com.example.myfirstapp" />
   </manifest>
   ```

3. 根目录下添加jacoco.gradle

   ```
   apply plugin: 'jacoco'
   
   jacoco {
       toolVersion = '0.8.2'
   }
   
   task jacocoTestReport(type: JacocoReport) {
       def classExcludes = ['**/R*.class',
                            '**/*Factory*.class',
                            '**/*$InjectAdapter*.class',
                            '**/*$ModuleAdapter*.class',
                            '**/*$ViewInjector*.class']
       group = "Reporting"
       description = "Generate Jacoco coverage reports after running tests."
       reports {
           xml.enabled = true
           html.enabled = true
       }
   
       def a=files()
       def b=files()
       project.rootProject.allprojects.each {
           a += files(it.projectDir.absolutePath + '/src/main/java')
           def path = it.buildDir.absolutePath + '/intermediates/javac/debug/classes/'
           b += fileTree(dir: path, excludes: classExcludes, includes: ['**/*.class'])
       }
       sourceDirectories.from(files(a))
       classDirectories.from(files(b))
       /**
        * 以下是比较笨的方法
        * 把每个module都手动录入
        * */
   // =================================================================================================
   //    sourceDirectories.from(
   //            files("/Users/happyelements/IdeaProjects/my-app/app/src/main/java"),
   //            files("/Users/happyelements/IdeaProjects/my-app/mylibrary/src/main/java")
   //    )
   //    classDirectories.from(
   //            fileTree(dir: "/Users/happyelements/IdeaProjects/my-app/app/build/intermediates/javac/debug/classes/",excludes: classExcludes),
   //            fileTree(dir: "/Users/happyelements/IdeaProjects/my-app/mylibrary/build/intermediates/javac/debug/classes/",excludes: classExcludes)
   //    )
   // =================================================================================================
       // coverage.ec从手机down下来的文件
       executionData.from(files("/Users/happyelements/IdeaProjects/my-app/jacoco/coverage.ec"))
   
       doFirst {
           fileTree(dir: project.rootDir.absolutePath, includes: ['**/classes/**/*.class']).each { File file ->
               if (file.name.contains('$$')) {
                   file.renameTo(file.path.replace('$$', '$'))
               }
           }
       }
   }
   ```

4. build.gradle

   添加```jacoco.gradle```到build.gradle

   ```apply from: "${rootProject.projectDir}/jacoco.gradle"```

5. 在idea右侧gradle，选择app>Tasks>install>installDebug，安装demo到设备上

   ![image-20210416164016465](/Users/happyelements/Library/Application Support/typora-user-images/image-20210416164016465.png)

6. 命令行执行```adb shell am instrument com.example.myfirstapp/com.example.test.JacocoInstrumentation```启动应用

7. 应用内执行完操作，退出应用

8. 查看设备目录```/data/data/com.example.myfirstapp/file```下是否有```coverage.ec```文件

   ![image-20210416164646538](/Users/happyelements/Library/Application Support/typora-user-images/image-20210416164646538.png)

9. 将```coverage.ec```文件存储到项目目录下```jacoco``文件夹下

10. 执行

    ![image-20210416165430699](/Users/happyelements/Library/Application Support/typora-user-images/image-20210416165430699.png)

11. 根目录查看build是否生成报告

    ![image-20210416165508227](/Users/happyelements/Library/Application Support/typora-user-images/image-20210416165508227.png)