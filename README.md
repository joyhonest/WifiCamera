# WifiCamera

1 在项目的主build.gradle 添加

  allprojects {
	  	repositories {
		  	...
			  maven { url 'https://jitpack.io' }
  		}
	  }

2 在模块的 build.gradle 添加
  dependencies {
	        implementation 'com.github.joyhonest:WifiCamera:4.6.1'
	}
  
3  本SDK使用了AndroidEventBus， 因为Jcenter 将失效将AndroidEventBus内置到SDK中。 所以使用者的项目中无需再 implementation 'org.simple:androideventbus:1.0.5.1'
   AndroidEventBus使用方法参见 https://github.com/hehonghui/AndroidEventBus 
