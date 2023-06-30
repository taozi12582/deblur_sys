#!/bin/bash

img_name="$1"
device="$2"
taskId="$3"

source /home/mscg/miniconda3/bin/activate
eval "$(conda shell.bash hook)"
conda activate taozi
python /stdStorage/taozi/deblur_sys/src/main/java/com/taozi/deblur/util/evaImg.py --img_name="$img_name" --device="$device" --taskId="$taskId"
