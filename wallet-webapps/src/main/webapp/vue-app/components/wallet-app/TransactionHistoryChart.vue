<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<script>
export default {
  extends: VueChart.Line,
  props: {
    transactionStatistics: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data () {
    return {
      incomeGradient: null,
      outcomeGradient: null,
    };
  },
  watch: {
    transactionStatistics() {
      this.initializeChart();
    }
  },
  methods: {
    initializeChart() {
      if (!this.transactionStatistics) {
        return;
      }
      this.incomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450);
      this.outcomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450);
  
      this.outcomeGradient.addColorStop(0, 'rgba(255, 0,0, 0.5)');
      this.outcomeGradient.addColorStop(0.5, 'rgba(255, 0, 0, 0.25)');
      this.outcomeGradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
      
      this.incomeGradient.addColorStop(0, 'rgba(0, 231, 255, 0.9)');
      this.incomeGradient.addColorStop(0.5, 'rgba(0, 231, 255, 0.25)');
      this.incomeGradient.addColorStop(1, 'rgba(0, 231, 255, 0)');

      this.renderChart({
        labels: this.transactionStatistics.labels,
        datasets: [
          {
            label: this.$t('exoplatform.wallet.chart.Income'),
            borderColor: '#05CBE1',
            pointBackgroundColor: 'white',
            pointBorderColor: 'white',
            borderWidth: 1,
            backgroundColor: this.incomeGradient,
            data: this.transactionStatistics.income,
          },{
            label: this.$t('exoplatform.wallet.chart.Outcome'),
            borderColor: '#FC2525',
            pointBackgroundColor: 'white',
            pointBorderColor: 'white',
            borderWidth: 1,
            backgroundColor: this.outcomeGradient,
            data: this.transactionStatistics.outcome,
          }
        ]
      }, {responsive: true, maintainAspectRatio: false,legend: {display: false}});
    }
  }
};

</script>
