.class public Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInteract;
.super Ljava/lang/Object;
.source "CameraLightInteract.java"


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private static buildLightsJson(Ljava/util/List;)Ljava/lang/String;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;",
            ">;)",
            "Ljava/lang/String;"
        }
    .end annotation

    invoke-interface {p0}, Ljava/util/List;->size()I

    move-result v0

    mul-int/lit8 v1, v0, 0x50

    add-int/lit8 v1, v1, 0x2

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v1}, Ljava/lang/StringBuilder;-><init>(I)V

    const/16 v3, 0x5b

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    const/4 v4, 0x0

    :loop_start
    if-lt v4, v0, :loop_body

    goto :loop_end

    :loop_body
    if-eqz v4, :not_first

    const/16 v3, 0x2c

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    :not_first
    invoke-interface {p0, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;

    const-string v6, "{\"status\":"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v7, v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->c:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v6, ",\"countdown\":"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v7, v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->d:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v6, ",\"dir\":"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v7, v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->b:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v6, ",\"waitNum\":"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v7, v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->a:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v6, ",\"showType\":"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v7, v5, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->e:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const/16 v6, 0x7d

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    add-int/lit8 v4, v4, 0x1

    goto :loop_start

    :loop_end
    const/16 v3, 0x5d

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static notifyCameraLightInfos(Landroid/os/Parcel;)V
    .locals 1

    const/4 v0, 0x0

    invoke-virtual {p0, v0}, Landroid/os/Parcel;->setDataPosition(I)V

    sget-object v0, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;->CREATOR:Landroid/os/Parcelable$Creator;

    invoke-interface {v0, p0}, Landroid/os/Parcelable$Creator;->createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;

    invoke-static {v0}, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInteract;->notifyCameraLightInfosImpl(Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;)V

    invoke-virtual {p0}, Landroid/os/Parcel;->recycle()V

    return-void
.end method

.method public static notifyCameraLightInfosImpl(Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;)V
    .locals 1

    invoke-static {}, Lvh0;->e()Lvh0;

    move-result-object v0

    invoke-virtual {v0, p0}, Lvh0;->a(Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;)V

    invoke-static {p0}, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInteract;->sendAmapCompanionCruiseBroadcast(Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;)V

    return-void
.end method

.method private static sendAmapCompanionCruiseBroadcast(Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;)V
    .locals 8

    :try_start
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;

    move-result-object v1

    if-eqz v1, :return_void

    new-instance v2, Landroid/content/Intent;

    const-string v3, "AUTONAVI_STANDARD_BROADCAST_SEND"

    invoke-direct {v2, v3}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    const-string v3, "KEY_TYPE"

    const v4, 0xeaa9

    invoke-virtual {v2, v3, v4}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    if-eqz p0, :send_clear

    iget-object v0, p0, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfoWrapper;->a:Ljava/util/List;

    if-eqz v0, :send_clear

    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    move-result v3

    if-nez v3, :send_clear

    const/4 v3, 0x0

    invoke-interface {v0, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;

    const-string v4, "trafficLightStatus"

    iget v5, v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->c:I

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "redLightCountDownSeconds"

    iget v5, v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->d:I

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "dir"

    iget v5, v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->b:I

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "waitRound"

    iget v5, v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->a:I

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "showType"

    iget v5, v3, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInfo;->e:I

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    invoke-static {v0}, Lcom/autonavi/amapauto/CameraLightInfo/CameraLightInteract;->buildLightsJson(Ljava/util/List;)Ljava/lang/String;

    move-result-object v6

    const-string v4, "lightsData"

    invoke-virtual {v2, v4, v6}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string v4, "lightsCount"

    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v7

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "clearLights"

    const/4 v7, 0x0

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;

    const-string v4, "EXTRA_CLEAR_LIGHTS"

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;

    invoke-virtual {v1, v2}, Landroid/app/Application;->sendBroadcast(Landroid/content/Intent;)V

    goto :return_void

    :send_clear
    const-string v4, "lightsData"

    const-string v5, "[]"

    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string v4, "lightsCount"

    const/4 v7, 0x0

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;

    const-string v4, "clearLights"

    const/4 v7, 0x1

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;

    const-string v4, "EXTRA_CLEAR_LIGHTS"

    invoke-virtual {v2, v4, v7}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;

    invoke-virtual {v1, v2}, Landroid/app/Application;->sendBroadcast(Landroid/content/Intent;)V

    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all

    goto :return_void

    :catch_all
    move-exception v0

    :return_void
    return-void
.end method
