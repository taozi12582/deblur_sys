import pyiqa
import torch
import os
import argparse


def get_score(img_name, device_num, img_dir):
    deblur_img = img_dir+'blur/'+img_name
    sharp_img = img_dir+'sharp/'+img_name
    device = torch.device("cuda:"+str(device_num)) if torch.cuda.is_available() else torch.device("cpu")
    niqe_metric = pyiqa.create_metric('niqe',device=device)
    psnr_metric = pyiqa.create_metric('psnr',device=device)
    ssim_metric = pyiqa.create_metric('ssim',device=device)
    niqe = niqe_metric(deblur_img)
    psnr = psnr_metric(deblur_img,sharp_img)
    ssim = ssim_metric(deblur_img,sharp_img)
    return psnr.item(), ssim.item(), niqe.item()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--img_name', type=str, default = None)
    parser.add_argument('--device', type=int, default = 3)
    parser.add_argument('--taskId', type=str, default = None)
    args = parser.parse_args()
#     print('calculating...')
    img_dir = '/stdStorage/taozi/deblur_sys/img/' + args.taskId + '/'
    res = get_score(args.img_name, args.device, img_dir)
    print(str(res[0])+','+str(res[1])+','+str(res[2]))
#     with open(args.img_name[:-4] + ".txt", "w") as f:
#         f.write(str(res[0])+','+str(res[1])+','+str(res[2]))
