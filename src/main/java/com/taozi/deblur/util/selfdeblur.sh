#!/bin/bash

blurdir="$1"
resdir="$2"
gpu="$3"

source /home/mscg/miniconda3/bin/activate
eval "$(conda shell.bash hook)"
conda activate selfdeblur
CUDA_VISIBLE_DEVICES="$gpu" python /home/tzx/selfDeblurPredict/mainPredict.py --blurdir="$blurdir" --resdir="$resdir"
