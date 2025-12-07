# LibImage Android

Customize images to send over mediums such as SMS. This is primary built for tranmission with RelaySMS, but can be customized for any project. 
The SMSWithoutBorders libsmsmms is used in creating a worker which can transmit the images provided by the customizations from here. 

The goal of this library is to provide the user with extreme customization features for reducing the size of an image for low bandwidth transmissions.

## Dependencies
Add to the root of your project
```gradle
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

Add to your dependencies
```gradle
dependencies {
	        implementation 'com.github.smswithoutborders:lib_image_android:Tag'
	}
```

## Usage
```kotlin
val imageViewModel: ImageViewModel by viewModels()

fun ImageRender(
    navController: NavController,
    imageViewModel: ImageViewModel,
    uri: Uri,
    maxNumberSms: Int = 64,
    smsCountPaddingValue: Int = 0,
    backActionCallback: () -> Unit = { navController.popBackStack() },
) {
...
{
```

## How it works
- **Auto adjust**
This uses a binary search to achieve the required number of messages to be sent; it also has a plus/minus one margin for the matching with the required size. You can use either `strict_upperbound` or `strict_lowerbound` to prefer the plus or minus margin respectively. If both are true, strict_upperbound is defaulted.
