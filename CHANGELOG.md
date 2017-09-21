## 0.5
* Move OxidMetadataFileInspection into PsiElementVisitor
* Migration to PhpTypeProvider3 for PhpStorm 2017.3 [#12](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/12)
* Fix com.intellij.openapi.project.IndexNotReadyException in custom trait generator for class overwrites [#10](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/10)

## 0.4
* Fix NullPointerException in visitMetadataKey [#5](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/5)
* fix modules without vendor are not in .phpstorm-oxid.meta.php [#2](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/2)
* Add cache for metadata getExtendsList, as this one is the slowest stuff

## 0.3.1
* Support core "lang.php" for translations
* Fix repeating File Cache Conflict since [#4](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/4)

## 0.3
* Implement traits generator for extend metadata class, so every module class knows foreign inheritance
* Add support oxmultilang::ident and oxcontent::ident in smarty

## 0.2.1
* Fix lookup elements for factory methods where collected in nowhere

## 0.2
* proxy getArrayKeyValueMap for npe fix [#1](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/1)
* Recursive search for module or vendor module structure [#2](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/2)
* Support array key of "extend" in metadata [#3](https://github.com/Haehnchen/idea-php-oxid-plugin/issues/3)
* Some performance improvements

## 0.1
* Initial release with dep on [Symfony2 Plugin](http://plugins.jetbrains.com/plugin/7219)