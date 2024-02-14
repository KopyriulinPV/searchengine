                                  Поисковый движок по сайтам.
Перед запуском приложения в файле application.yaml в indexing-settings 
пропишите url и name сайтов, которые планируете индексировать. В файле application.yaml введите свои данные 
для БД MySQL "username, password, url".

![img_7.png](img_7.png)

![img_9.png](img_9.png)

Запустите приложение и откройте его через браузер по адресу: http://localhost:8080/.



![img_2.png](img_2.png)

Индексакция сайтов осуществляется в многопоточном режиме с использованием ForkJoinPool.

![img_3.png](img_3.png)
![img_4.png](img_4.png)
![img_5.png](img_5.png)