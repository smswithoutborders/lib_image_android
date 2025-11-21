# LibImage Android

Customize images to send over mediums such as SMS. This is primary built for tranmission with RelaySMS, but can be customized for any project. 
The SMSWithoutBorders libsmsmms is used in creating a worker which can transmit the images provided by the customizations from here. 

The goal of this library is to provide the user with extreme customization features for reducing the size of an image for low bandwidth transmissions.

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
