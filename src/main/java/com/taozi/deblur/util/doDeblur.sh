#!/bin/bash

input_path="$1"
output_path="$2"
gpu="$3"

echo "$input_path"
echo "$output_path"
echo "$gpu"
source /home/mscg/miniconda3/bin/activate
eval "$(conda shell.bash hook)"
conda activate /stdStorage/taozi/condaEnv/envs_dirs/srn
python /stdStorage/taozi/SRN-Deblur-master/run_model.py --input_path="$input_path" --output_path="$output_path" --gpu="$gpu"
